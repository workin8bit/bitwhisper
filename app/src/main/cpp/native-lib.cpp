#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeTranscribe(JNIEnv* env, jobject, jstring, jstring, jstring) {
    // TODO: link Whisper.cpp and run inference here.
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_bitwhisper_NativeInference_nativeChat(JNIEnv* env, jobject, jstring, jstring prompt, jint) {
    // TODO: link llama.cpp and run GGUF inference here.
    return env->NewStringUTF("");
}
