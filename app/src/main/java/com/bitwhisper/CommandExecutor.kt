package com.bitwhisper

import android.content.Context
import android.content.Intent
import android.provider.Settings

class CommandExecutor(private val context: Context) {
    private fun controlCall(action: String): String {
        val service = BitWhisperAccessibilityService.active ?: return "Aku belum bisa mengontrol panggilan sekarang."
        val labels = when (action) {
            "answer" -> listOf("Jawab", "Answer", "Angkat")
            "reject" -> listOf("Tolak", "Decline", "Reject")
            "speaker_on" -> listOf("Speaker", "Loudspeaker")
            else -> listOf("Speaker", "Loudspeaker", "Earpiece")
        }
        val executor = UiActionExecutor(service)
        val clicked = labels.any { executor.clickConfirmed(it) }
        return if (clicked) when (action) {
            "answer" -> "Oke, aku angkat."
            "reject" -> "Oke, aku tolak."
            "speaker_on" -> "Oke, speaker nyala."
            else -> "Oke, speaker aku matikan."
        } else "Aku belum menemukan tombol panggilan itu."
    }

    fun execute(result: IntentResult): String = when (result) {
        is IntentResult.Dictation -> result.text
        is IntentResult.CallControl -> controlCall(result.action)
        is IntentResult.CreateNote -> "Catatan dibuat: ${result.body}"
        is IntentResult.Timer -> "Timer ${result.minutes} menit disiapkan"
        is IntentResult.Flashlight -> "Kontrol flashlight akan dijalankan"
        is IntentResult.OpenApp -> {
            if (result.name == "__settings__") {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Oke, aku buka pengaturan."
            } else {
                val launch = context.packageManager.getLaunchIntentForPackage(result.name)
                if (launch != null) { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Oke, aku buka ${result.name}." }
                else "Aku nggak menemukan aplikasi ${result.name}."
            }
        }
    }
}
