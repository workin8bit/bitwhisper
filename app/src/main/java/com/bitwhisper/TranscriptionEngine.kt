package com.bitwhisper

import android.content.Context
import java.io.File

interface TranscriptionEngine {
    fun transcribe(pcm16File: File): String
}

class LocalWhisperEngine(private val context: Context) : TranscriptionEngine {
    private val models = ModelManager(context)
    override fun transcribe(pcm16File: File): String {
        require(pcm16File.exists()) { "Audio file does not exist" }
        if (!models.isWhisperReady() || !NativeInference.isAvailable()) return ""
        val wav = File(pcm16File.parentFile, "last-recording.wav")
        PcmAudio.toWav(pcm16File, wav)
        val language = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("input_language", "auto") ?: "auto"
        return NativeInference.transcribe(models.whisperModel, wav, language)
    }
}
