#include <jni.h>
#include <whisper.h>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>

static bool read_wav_pcm16(const char * path, std::vector<float> & out) {
    std::ifstream file(path, std::ios::binary);
    if (!file) return false;
    char header[44]; file.read(header, 44);
    if (file.gcount() != 44 || header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[8] != 'W') return false;
    std::vector<int16_t> samples((std::istreambuf_iterator<char>(file)), {});
    if (samples.empty()) return false;
    out.resize(samples.size() / 2);
    file.clear(); file.seekg(44);
    for (size_t i = 0; i < out.size(); ++i) {
        unsigned char lo = 0, hi = 0; file.read(reinterpret_cast<char *>(&lo), 1); file.read(reinterpret_cast<char *>(&hi), 1);
        int16_t sample = static_cast<int16_t>(static_cast<uint16_t>(lo) | (static_cast<uint16_t>(hi) << 8));
        out[i] = static_cast<float>(sample) / 32768.0f;
    }
    return true;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeTranscribe(JNIEnv* env, jobject, jstring modelJ, jstring audioJ, jstring languageJ) {
    const char * model = env->GetStringUTFChars(modelJ, nullptr);
    const char * audio = env->GetStringUTFChars(audioJ, nullptr);
    const char * language = env->GetStringUTFChars(languageJ, nullptr);
    std::vector<float> pcm;
    whisper_context_params cp = whisper_context_default_params();
    whisper_context * ctx = whisper_init_from_file_with_params(model, cp);
    std::string result;
    if (ctx && read_wav_pcm16(audio, pcm)) {
        whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        params.print_progress = false; params.print_realtime = false; params.print_timestamps = false;
        params.translate = false; params.language = language;
        if (whisper_full(ctx, params, pcm.data(), pcm.size()) == 0) {
            for (int i = 0; i < whisper_full_n_segments(ctx); ++i) result += whisper_full_get_segment_text(ctx, i);
        }
    }
    if (ctx) whisper_free(ctx);
    env->ReleaseStringUTFChars(modelJ, model); env->ReleaseStringUTFChars(audioJ, audio); env->ReleaseStringUTFChars(languageJ, language);
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeChat(JNIEnv* env, jobject, jstring, jstring, jint) {
    return env->NewStringUTF("Chatbot native belum terhubung.");
}
