package com.bitwhisper

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class ResponseSpeaker(private val context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var ready = false
    override fun onInit(status: Int) { ready = status == TextToSpeech.SUCCESS; if (ready) tts.language = Locale("id", "ID") }
    fun speak(text: String) {
        val mode = preferences.getString("output_mode", "both")
        if (ready && mode != "text") tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bitwhisper-response")
    }
    fun close() { tts.shutdown() }
}
