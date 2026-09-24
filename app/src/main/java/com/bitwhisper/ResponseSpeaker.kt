package com.bitwhisper

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class ResponseSpeaker(private val context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var ready = false
    private var pendingReadyAnnouncement = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val indonesia = Locale("id", "ID")
            tts.language = indonesia
            // Prefer an Indonesian voice whose engine metadata identifies it as
            // masculine. Engines differ in naming, so retain the normal locale
            // voice as a safe fallback.
            tts.voices?.firstOrNull { voice ->
                voice.locale.language == "id" &&
                    (voice.name.contains("male", true) || voice.name.contains("pria", true) || voice.name.contains("masculine", true))
            }?.let { tts.voice = it }
            // A slightly lower pitch and a natural pace give the voice a
            // friendly, lightly robotic character without sounding formal.
            tts.setPitch(0.92f)
            tts.setSpeechRate(1.02f)
            if (pendingReadyAnnouncement) speak("Oke, aku siap. Tinggal tahan Volume Up kalau mau ngobrol.")
        }
    }

    fun announceReady() {
        pendingReadyAnnouncement = true
        if (ready) speak("Oke, aku siap. Tinggal tahan Volume Up kalau mau ngobrol.")
    }

    fun speak(text: String) {
        val mode = preferences.getString("output_mode", "both")
        if (ready && mode != "text") tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bitwhisper-response")
    }

    fun close() { tts.shutdown() }
}
