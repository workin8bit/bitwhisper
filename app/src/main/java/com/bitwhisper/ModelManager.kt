package com.bitwhisper

import android.content.Context
import java.io.File

class ModelManager(context: Context) {
    private val directory = File(context.filesDir, "models")
    val tinyWhisperModel = File(directory, "ggml-tiny.bin")
    val baseWhisperModel = File(directory, "ggml-base.bin")
    // Kept for compatibility with model status screens.
    val whisperModel: File get() = if (tinyReady()) tinyWhisperModel else baseWhisperModel
    val chatModel = File(directory, "qwen2.5-1.5b-instruct-q4_k_m.gguf")

    fun tinyReady() = tinyWhisperModel.exists() && tinyWhisperModel.length() > 1_000_000
    fun baseReady() = baseWhisperModel.exists() && baseWhisperModel.length() > 1_000_000
    // Audio duration is available before transcription, so it is a reliable
    // proxy for command length. Tiny keeps short commands responsive; Base
    // preserves accuracy for longer speech.
    fun whisperModelFor(audio: File): File {
        val durationSeconds = audio.length().toDouble() / (16000.0 * 2.0)
        return when {
            durationSeconds <= 4.0 && tinyReady() -> tinyWhisperModel
            baseReady() -> baseWhisperModel
            tinyReady() -> tinyWhisperModel
            else -> baseWhisperModel
        }
    }
    fun isWhisperReady() = tinyReady() || baseReady()
    fun isChatReady() = chatModel.exists() && chatModel.length() > 1_000_000
    fun ensureDirectory() { directory.mkdirs() }
}
