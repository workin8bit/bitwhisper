package com.bitwhisper

import android.content.Context
import android.util.Log
import java.io.File

interface TranscriptionEngine {
    fun transcribe(pcm16File: File): String
}

class LocalWhisperEngine(private val context: Context) : TranscriptionEngine {
    private val models = ModelManager(context)
    override fun transcribe(pcm16File: File): String {
        require(pcm16File.exists()) { "Audio file does not exist" }
        if (!models.isWhisperReady()) { Log.e(TAG, "Whisper model not ready: ${models.whisperModel}"); return "" }
        if (!NativeInference.isAvailable()) { Log.e(TAG, "Native inference library is unavailable"); return "" }
        val wav = File(pcm16File.parentFile, "last-recording.wav")
        PcmAudio.toWav(pcm16File, wav)
        val language = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("input_language", "auto") ?: "auto"
        Log.i(TAG, "Starting native Whisper: wav=${wav.length()} model=${models.whisperModel.length()} language=$language")
        return runCatching { NativeInference.transcribe(models.whisperModel, wav, language) }
            .also { Log.i(TAG, "Native Whisper returned length=${it.getOrDefault("").length}") }
            .onFailure { Log.e(TAG, "Whisper transcription failed", it) }
            .getOrDefault("")
    }
    companion object { private const val TAG = "BitWhisperWhisper" }
}
