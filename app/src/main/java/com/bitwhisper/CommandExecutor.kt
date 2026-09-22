package com.bitwhisper

import android.content.Context
import android.content.Intent
import android.provider.Settings

class CommandExecutor(private val context: Context) {
    fun execute(result: IntentResult): String = when (result) {
        is IntentResult.Dictation -> result.text
        is IntentResult.CreateNote -> "Catatan dibuat: ${result.body}"
        is IntentResult.Timer -> "Timer ${result.minutes} menit disiapkan"
        is IntentResult.Flashlight -> "Kontrol flashlight akan dijalankan"
        is IntentResult.OpenApp -> {
            val launch = context.packageManager.getLaunchIntentForPackage(result.name)
            if (launch != null) { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Membuka ${result.name}" }
            else "Aplikasi ${result.name} tidak ditemukan"
        }
    }
}
