package com.bitwhisper

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        layout.addView(TextView(this).apply { text = "BitWhisper\nAsisten suara offline untuk Infinix GT 30 Pro"; textSize = 22f })
        layout.addView(TextView(this).apply { text = "Aktifkan izin berikut agar trigger Volume Up dapat bekerja di background."; setPadding(0, 24, 0, 24) })
        val models = ModelManager(this)
        val wakeModel = WakeWordModelManager(this)
        layout.addView(TextView(this).apply {
            text = "Model Whisper: ${if (models.isWhisperReady()) "siap" else "belum dipasang"}\nModel chatbot: ${if (models.isChatReady()) "siap" else "belum dipasang"}\nModel wake word: ${if (wakeModel.isReady()) "siap" else "belum dipasang"}"
            setPadding(0, 0, 0, 16)
        })
        val modelStatus = TextView(this).apply { text = "" }
        layout.addView(modelStatus)
        val downloader = ModelDownloader(this)
        layout.addView(button("Unduh model Whisper") { modelStatus.text = "Mengunduh Whisper..."; downloader.downloadWhisper({ p -> runOnUiThread { modelStatus.text = "Whisper: $p%" } }) { r -> runOnUiThread { modelStatus.text = if (r.isSuccess) "Whisper selesai" else "Whisper gagal: ${r.exceptionOrNull()?.message}" } } })
        layout.addView(button("Unduh model chatbot") { modelStatus.text = "Mengunduh chatbot..."; downloader.downloadChat({ p -> runOnUiThread { modelStatus.text = "Chatbot: $p%" } }) { r -> runOnUiThread { modelStatus.text = if (r.isSuccess) "Chatbot selesai" else "Chatbot gagal: ${r.exceptionOrNull()?.message}" } } })
        layout.addView(button("Izinkan mikrofon") { requestMic() })
        if (android.os.Build.VERSION.SDK_INT >= 33) layout.addView(button("Izinkan notifikasi") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11) })
        layout.addView(button("Aktifkan Accessibility Service") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        layout.addView(button("Atur baterai tanpa pembatasan") { openBatterySettings() })
        layout.addView(TextView(this).apply { text = "Output jawaban"; textSize = 18f; setPadding(0, 20, 0, 8) })
        layout.addView(button("Text saja") { setOutputMode("text") })
        layout.addView(button("Voice saja") { setOutputMode("voice") })
        layout.addView(button("Text + Voice") { setOutputMode("both") })
        layout.addView(TextView(this).apply { text = "Bahasa input"; textSize = 18f; setPadding(0, 20, 0, 8) })
        layout.addView(button("Otomatis") { setLanguage("auto") })
        layout.addView(button("Indonesia") { setLanguage("id") })
        layout.addView(button("Jawa") { setLanguage("jv") })
        layout.addView(TextView(this).apply { text = "Mode chatbot"; textSize = 18f; setPadding(0, 20, 0, 8) })
        layout.addView(button("Umum") { ChatSettings(this).setMode(ChatMode.GENERAL) })
        layout.addView(button("Scientific") { ChatSettings(this).setMode(ChatMode.SCIENTIFIC) })
        layout.addView(TextView(this).apply { text = "Wake word"; textSize = 18f; setPadding(0, 20, 0, 8) })
        val wakeWordInput = EditText(this).apply { hint = "Contoh: kyu kyu"; setText(getSharedPreferences("settings", MODE_PRIVATE).getString("wake_word", "kyu kyu")) }
        layout.addView(wakeWordInput)
        layout.addView(button("Simpan wake word") { saveWakeWord(wakeWordInput.text.toString()) })
        layout.addView(button("Aktifkan wake word") { setWakeWordEnabled(true) })
        layout.addView(button("Matikan wake word") { setWakeWordEnabled(false) })
        layout.addView(button("Mulai BitWhisper") { startForegroundService(Intent(this, VoiceService::class.java)) })
        layout.addView(TextView(this).apply { text = "Riwayat percakapan"; textSize = 18f; setPadding(0, 24, 0, 8) })
        val historyText = TextView(this)
        layout.addView(historyText)
        layout.addView(button("Muat ulang riwayat") { historyText.text = historyText() })
        layout.addView(button("Hapus riwayat") { ConversationStore(this).clear(); historyText.text = "Belum ada percakapan." })
        historyText.text = historyText()
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun historyText(): String {
        val entries = ConversationStore(this).all()
        return if (entries.isEmpty()) "Belum ada percakapan." else entries.takeLast(20).joinToString("\n\n") { "Anda: ${it.user}\nBitWhisper: ${it.assistant}" }
    }

    private fun setOutputMode(mode: String) = getSharedPreferences("settings", MODE_PRIVATE).edit().putString("output_mode", mode).apply()
    private fun setLanguage(language: String) = getSharedPreferences("settings", MODE_PRIVATE).edit().putString("input_language", language).apply()
    private fun setWakeWordEnabled(enabled: Boolean) = getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("wake_word_enabled", enabled).apply()
    private fun saveWakeWord(value: String) { if (value.trim().length >= 3) getSharedPreferences("settings", MODE_PRIVATE).edit().putString("wake_word", value.trim()).apply() }
    private fun requestMic() = requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
    private fun openLink(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }
        runCatching { startActivity(intent) }.getOrElse { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}
