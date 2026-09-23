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
            .replace(Regex("[.!?,']"), "")
            .replace("what s up", "whatsapp")
            .replace("whats up", "whatsapp")
            .replace("what up", "whatsapp")
            .replace("what s app", "whatsapp")
            .replace("whats app", "whatsapp")
            .replace("wads app", "whatsapp")
            .replace("wadsap", "whatsapp")
            .replace("wats app", "whatsapp")
            .replace("what app", "whatsapp")
            .replace("wa sap", "whatsapp")
            .replace("wasap", "whatsapp")
            .replace("watsap", "whatsapp")
        val aliases = mapOf(
            "whatsapp" to "com.whatsapp",
            "telegram" to "org.telegram.messenger",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome"
        )
        aliases[normalized]?.let { alias ->
            if (runCatching { context.packageManager.getPackageInfo(alias, 0) }.isSuccess) return alias
        }
        val apps = context.packageManager.getInstalledApplications(0)
            .filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
        apps.firstOrNull {
            context.packageManager.getApplicationLabel(it).toString().lowercase().contains(normalized)
        }?.packageName?.let { return it }

        // Whisper can produce small spelling/phonetic errors. Use a conservative
        // edit-distance fallback for any installed launchable app, but do not
        // guess when the name is too different.
        val best = apps.map {
            val label = context.packageManager.getApplicationLabel(it).toString().lowercase()
            it to editDistance(normalized, label)
        }.minByOrNull { it.second }
        val maxDistance = maxOf(2, normalized.length / 3)
        return best?.takeIf { it.second <= maxDistance }?.first?.packageName
    }

    private fun editDistance(a: String, b: String): Int {
        val previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + if (a[i] == b[j]) 0 else 1
            )
            for (j in previous.indices) previous[j] = current[j]
        }
        return previous[b.length]
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
                var launch = packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }
                // Some XOS builds hide launcher intents from getLaunchIntentForPackage.
                // Resolve the explicit launcher activity as a fallback.
                if (launch == null && packageName == "com.whatsapp") {
                    launch = Intent().setComponent(android.content.ComponentName("com.whatsapp", "com.whatsapp.Main"))
                }
                if (launch != null) { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "Oke, aku buka ${result.name}." }
                else "Aku nggak menemukan aplikasi ${result.name}."
            }
        }
    }
}
