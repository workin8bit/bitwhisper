#include <jni.h>
#include <whisper.h>
#include <android/log.h>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <mutex>

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
        params.translate = false; params.language = language; params.n_threads = 4;
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeChat(JNIEnv* env, jobject, jstring, jstring, jint) {
    return env->NewStringUTF("Chatbot native belum terhubung.");
}
