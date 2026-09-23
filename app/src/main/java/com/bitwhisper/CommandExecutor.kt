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

    private fun resolvePackage(query: String): String? {
        val normalized = query.trim().lowercase()
            .replace(Regex("[.!?,]"), "")
            .replace("wa sap", "whatsapp")
            .replace("wasap", "whatsapp")
            .replace("watsap", "whatsapp")
        val aliases = mapOf(
            "whatsapp" to "com.whatsapp",
            "telegram" to "org.telegram.messenger",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome"
        )
        aliases[normalized]?.let { if (context.packageManager.getLaunchIntentForPackage(it) != null) return it }
        return context.packageManager.getInstalledApplications(0).firstOrNull {
            context.packageManager.getApplicationLabel(it).toString().lowercase().contains(normalized)
        }?.packageName
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
                val packageName = resolvePackage(result.name)
                val launch = packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
                if (launch != null) { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Oke, aku buka ${result.name}." }
                else "Aku nggak menemukan aplikasi ${result.name}."
            }
        }
    }
}
