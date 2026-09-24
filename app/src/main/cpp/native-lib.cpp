#include <jni.h>
#include <whisper.h>
#include <android/log.h>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <mutex>
#include <llama.h>

#define BW_LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NativeInference", __VA_ARGS__)
#define BW_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NativeInference", __VA_ARGS__)

static std::mutex g_whisper_mutex;
static whisper_context * g_context = nullptr;
static std::string g_model_path;

static bool read_wav_pcm16(const char * path, std::vector<float> & out) {
    std::ifstream file(path, std::ios::binary);
    if (!file) return false;
    char header[44]; file.read(header, 44);
    if (file.gcount() != 44 || header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[8] != 'W') return false;
    file.seekg(0, std::ios::end);
    const std::streamoff end = static_cast<std::streamoff>(file.tellg());
    const std::streamoff bytes = end - static_cast<std::streamoff>(44);
    if (bytes <= 0 || (bytes % 2) != 0) return false;
    out.resize(static_cast<size_t>(bytes / 2));
    file.seekg(44);
    for (float & value : out) {
        unsigned char raw[2];
        file.read(reinterpret_cast<char *>(raw), 2);
        const int16_t sample = static_cast<int16_t>(static_cast<uint16_t>(raw[0]) | (static_cast<uint16_t>(raw[1]) << 8));
        value = static_cast<float>(sample) / 32768.0f;
    }
    return true;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeTranscribe(JNIEnv* env, jobject, jstring modelJ, jstring audioJ, jstring languageJ) {
    const char * model = env->GetStringUTFChars(modelJ, nullptr);
    const char * audio = env->GetStringUTFChars(audioJ, nullptr);
    const char * language = env->GetStringUTFChars(languageJ, nullptr);
    std::lock_guard<std::mutex> lock(g_whisper_mutex);
    BW_LOGI("nativeTranscribe begin audio=%s", audio);

    if (!g_context || g_model_path != model) {
        if (g_context) whisper_free(g_context);
        whisper_context_params cp = whisper_context_default_params();
        cp.use_gpu = false;
        BW_LOGI("loading Whisper model (first use or model changed)");
        g_context = whisper_init_from_file_with_params(model, cp);
        g_model_path = g_context ? model : "";
        BW_LOGI("Whisper model loaded=%s", g_context ? "yes" : "no");
    } else {
        BW_LOGI("reusing cached Whisper model");
    }

    std::vector<float> pcm;
    std::string result;
    if (g_context && read_wav_pcm16(audio, pcm)) {
        whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.print_progress = false; params.print_realtime = false; params.print_timestamps = false;
        params.translate = false; params.language = language; params.n_threads = 8;
        params.no_context = true;
        BW_LOGI("running inference samples=%zu", pcm.size());
        const int rc = whisper_full(g_context, params, pcm.data(), pcm.size());
        BW_LOGI("inference returned rc=%d segments=%d", rc, rc == 0 ? whisper_full_n_segments(g_context) : 0);
        if (rc == 0) for (int i = 0; i < whisper_full_n_segments(g_context); ++i) result += whisper_full_get_segment_text(g_context, i);
    } else {
        BW_LOGE("unable to load model or read WAV");
    }
    env->ReleaseStringUTFChars(modelJ, model); env->ReleaseStringUTFChars(audioJ, audio); env->ReleaseStringUTFChars(languageJ, language);
    BW_LOGI("nativeTranscribe end resultLength=%zu", result.size());
    return env->NewStringUTF(result.c_str());
}

static std::mutex g_chat_mutex;
static llama_model * g_chat_model = nullptr;
static std::string g_chat_model_path;

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeChat(JNIEnv* env, jobject, jstring modelJ, jstring promptJ, jint maxTokens) {
    const char * modelPath = env->GetStringUTFChars(modelJ, nullptr);
    const char * prompt = env->GetStringUTFChars(promptJ, nullptr);
    std::lock_guard<std::mutex> lock(g_chat_mutex);
    llama_backend_init();
    if (!g_chat_model || g_chat_model_path != modelPath) {
        if (g_chat_model) llama_model_free(g_chat_model);
        llama_model_params mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        BW_LOGI("BitWhisperChat: loading model");
        g_chat_model = llama_model_load_from_file(modelPath, mp);
        g_chat_model_path = g_chat_model ? modelPath : "";
        BW_LOGI("BitWhisperChat: model loaded=%s", g_chat_model ? "yes" : "no");
    }
    std::string result;
    if (g_chat_model) {
        llama_context_params cp = llama_context_default_params();
        cp.n_ctx = 1024; cp.n_batch = 256; cp.n_threads = 8; cp.n_threads_batch = 8;
        llama_context * ctx = llama_init_from_model(g_chat_model, cp);
        if (ctx) {
            const int n = -llama_tokenize(llama_model_get_vocab(g_chat_model), prompt, -1, nullptr, 0, true, true);
            std::vector<llama_token> tokens(static_cast<size_t>(n));
            llama_tokenize(llama_model_get_vocab(g_chat_model), prompt, -1, tokens.data(), n, true, true);
            llama_batch batch = llama_batch_init(n + 1, 0, 1);
            for (int i = 0; i < n; ++i) { batch.token[i] = tokens[i]; batch.pos[i] = i; batch.n_seq_id[i] = 1; batch.seq_id[i][0] = 0; batch.logits[i] = (i == n - 1); }
            BW_LOGI("BitWhisperChat: generation started");
            if (llama_decode(ctx, batch) == 0) {
                auto * sampler = llama_sampler_init_greedy();
                const int limit = maxTokens > 0 ? maxTokens : 128;
                for (int i = 0; i < limit; ++i) {
                    const llama_token tok = llama_sampler_sample(sampler, ctx, -1);
                    if (llama_vocab_is_eog(llama_model_get_vocab(g_chat_model), tok)) break;
                    char piece[256]; const int len = llama_token_to_piece(llama_model_get_vocab(g_chat_model), tok, piece, sizeof(piece), 0, true);
                    if (len > 0) result.append(piece, len);
                    batch.token[0] = tok; batch.pos[0] = n + i; batch.n_seq_id[0] = 1; batch.seq_id[0][0] = 0; batch.logits[0] = true;
                    if (llama_decode(ctx, batch) != 0) break;
                }
                llama_sampler_free(sampler);
            }
            llama_batch_free(batch); llama_free(ctx);
            BW_LOGI("BitWhisperChat: generation completed length=%zu", result.size());
        }
    }
    env->ReleaseStringUTFChars(modelJ, modelPath); env->ReleaseStringUTFChars(promptJ, prompt);
    return env->NewStringUTF(result.c_str());
}
