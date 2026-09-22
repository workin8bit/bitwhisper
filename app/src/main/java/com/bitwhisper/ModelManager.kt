package com.bitwhisper

import android.content.Context
import java.io.File

class ModelManager(context: Context) {
    private val directory = File(context.filesDir, "models")
    val tinyWhisperModel = File(directory, "ggml-tiny.bin")
    val baseWhisperModel = File(directory, "ggml-base.bin")
    // Prefer tiny for responsiveness, while keeping base as a compatible fallback.
    val whisperModel: File get() = if (tinyWhisperModel.exists() && tinyWhisperModel.length() > 1_000_000) tinyWhisperModel else baseWhisperModel
    val chatModel = File(directory, "qwen2.5-1.5b-instruct-q4_k_m.gguf")

    fun isWhisperReady() = whisperModel.exists() && whisperModel.length() > 1_000_000
    fun isChatReady() = chatModel.exists() && chatModel.length() > 1_000_000
    fun ensureDirectory() { directory.mkdirs() }
}
