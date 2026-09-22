package com.bitwhisper

import java.io.File

/** Optional JNI boundary. The app remains usable while native libraries/models are not installed. */
object NativeInference {
    private var loaded = false
    init { loaded = runCatching { System.loadLibrary("bitwhisper_native"); true }.getOrDefault(false) }

    fun isAvailable(): Boolean = loaded
    fun transcribe(model: File, audioWav: File, language: String): String {
        check(loaded) { "Native inference library is not installed" }
        check(model.exists()) { "Whisper model is missing" }
        check(audioWav.exists()) { "Audio file is missing" }
        return nativeTranscribe(model.absolutePath, audioWav.absolutePath, language)
    }
    fun chat(model: File, prompt: String, maxTokens: Int = 256): String {
        check(loaded) { "Native inference library is not installed" }
        check(model.exists()) { "Chat model is missing" }
        return nativeChat(model.absolutePath, prompt, maxTokens)
    }
    private external fun nativeTranscribe(model: String, audio: String, language: String): String
    private external fun nativeChat(model: String, prompt: String, maxTokens: Int): String
}
