package com.bitwhisper

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout
    private lateinit var dashboard: LinearLayout
    private lateinit var historyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(248,249,252)) }
        val header = LinearLayout(this).apply { setPadding(28, 28, 28, 16); gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "BitWhisper"; textSize = 24f; setTextColor(Color.rgb(25,35,55)) }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("⚙ Settings") { showSettings() })
        root.addView(header)
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        showDashboard()
    }

    private fun showDashboard() {
        content.removeAllViews()
        dashboard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 24) }
        dashboard.addView(TextView(this).apply { text = "Percakapan"; textSize = 28f; setTextColor(Color.rgb(25,35,55)); setPadding(0, 8, 0, 8) })
        dashboard.addView(TextView(this).apply { text = "Tanya apa saja secara offline dengan BitWhisper"; textSize = 15f; setTextColor(Color.DKGRAY); setPadding(0, 0, 0, 18) })
        historyText = TextView(this).apply { textSize = 16f; setTextColor(Color.rgb(35,40,50)); setPadding(18, 18, 18, 18); setBackgroundColor(Color.WHITE) }
        dashboard.addView(ScrollView(this).apply { addView(historyText) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val input = EditText(this).apply { hint = "Ketik pesan..."; setSingleLine(false); setPadding(18, 12, 18, 12) }
        val send = button("Kirim") { sendChat(input) }
        val composer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 12, 0, 0) }
        composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f)); composer.addView(send)
        dashboard.addView(composer)
        content.addView(ScrollView(this).apply { addView(dashboard) })
        refreshHistory()
    }

    private fun sendChat(input: EditText) {
        val prompt = input.text.toString().trim(); if (prompt.isEmpty()) return
        input.text.clear(); historyText.text = "BitWhisper sedang berpikir..."
        Thread {
            val answer = LocalChatEngine(this).respond(prompt, ConversationStore(this).all().takeLast(4).map { it.user to it.assistant }, ChatSettings(this).mode())
            ConversationStore(this).add(prompt, answer)
            runOnUiThread { refreshHistory() }
        }.start()
    }

    private fun refreshHistory() { historyText.text = historyTextValue() }
    private fun historyTextValue(): String = ConversationStore(this).all().takeLast(30).joinToString("\n\n") { "Anda\n${it.user}\n\nBitWhisper\n${it.assistant}" }.ifEmpty { "Belum ada percakapan.\n\nMulai dengan pertanyaan di bawah." }

    private fun showSettings() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 32) }
        layout.addView(TextView(this).apply { text = "Pengaturan"; textSize = 28f; setPadding(0, 0, 0, 16) })
        val models = ModelManager(this); val wakeModel = WakeWordModelManager(this); val status = TextView(this).apply { text = "Whisper: ${if (models.isWhisperReady()) "siap" else "belum"}\nChatbot: ${if (models.isChatReady()) "siap" else "belum"}\nWake word: ${if (wakeModel.isReady()) "siap" else "belum"}" }
        layout.addView(status); val modelStatus = TextView(this); layout.addView(modelStatus); val downloader = ModelDownloader(this)
        layout.addView(button("Unduh model Whisper") { modelStatus.text="Mengunduh Whisper..."; downloader.downloadWhisper({ p -> runOnUiThread { modelStatus.text="Whisper: $p%" } }) { runOnUiThread { modelStatus.text = if(it.isSuccess) "Whisper selesai" else "Whisper gagal" } } })
        layout.addView(button("Unduh model chatbot") { modelStatus.text="Mengunduh chatbot..."; downloader.downloadChat({ p -> runOnUiThread { modelStatus.text="Chatbot: $p%" } }) { runOnUiThread { modelStatus.text = if(it.isSuccess) "Chatbot selesai" else "Chatbot gagal" } } })
        layout.addView(button("Izinkan mikrofon") { requestMic() }); if (android.os.Build.VERSION.SDK_INT >= 33) layout.addView(button("Izinkan notifikasi") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11) })
        layout.addView(button("Aktifkan Accessibility Service") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }); layout.addView(button("Atur baterai tanpa pembatasan") { openBatterySettings() })
        layout.addView(TextView(this).apply { text="Output jawaban"; textSize=18f; setPadding(0,20,0,8) }); listOf("Text saja" to "text", "Voice saja" to "voice", "Text + Voice" to "both").forEach { layout.addView(button(it.first) { setOutputMode(it.second) }) }
        layout.addView(TextView(this).apply { text="Bahasa input"; textSize=18f; setPadding(0,20,0,8) }); listOf("Otomatis" to "auto", "Indonesia" to "id", "Jawa" to "jv").forEach { layout.addView(button(it.first) { setLanguage(it.second) }) }
        layout.addView(TextView(this).apply { text="Mode chatbot"; textSize=18f; setPadding(0,20,0,8) }); layout.addView(button("Umum") { ChatSettings(this).setMode(ChatMode.GENERAL) }); layout.addView(button("Scientific") { ChatSettings(this).setMode(ChatMode.SCIENTIFIC) })
        layout.addView(TextView(this).apply { text="Wake word"; textSize=18f; setPadding(0,20,0,8) }); val wake = EditText(this).apply { hint="hey bro"; setText(getSharedPreferences("settings",0).getString("wake_word","hey bro")) }; layout.addView(wake); layout.addView(button("Simpan wake word") { saveWakeWord(wake.text.toString()) })
        val enrollment=WakeWordEnrollmentStore(this); val enrollStatus=TextView(this).apply{text="Sample hey bro: ${enrollment.sampleCount}/3"}; layout.addView(enrollStatus); layout.addView(button("Rekam sample hey bro") { enrollStatus.text="Ucapkan hey bro sekarang..."; Thread { runCatching { enrollment.addPcmSample(WakeWordEnrollmentStore.recordSample()) }.onSuccess { runOnUiThread { enrollStatus.text="Sample hey bro: ${enrollment.sampleCount}/3" } }.onFailure { runOnUiThread { enrollStatus.text="Gagal merekam sample" } } }.start() }); layout.addView(button("Hapus sample wake word") { enrollment.clear(); enrollStatus.text="Sample hey bro: 0/3" })
        layout.addView(button("Aktifkan wake word") { setWakeWordEnabled(true) }); layout.addView(button("Matikan wake word") { setWakeWordEnabled(false) }); layout.addView(button("Mulai BitWhisper") { startForegroundService(Intent(this,VoiceService::class.java)) }); layout.addView(button("Kembali ke dashboard") { showDashboard() })
        content.addView(ScrollView(this).apply { addView(layout) })
    }

    private fun setOutputMode(v:String)=getSharedPreferences("settings",0).edit().putString("output_mode",v).apply(); private fun setLanguage(v:String)=getSharedPreferences("settings",0).edit().putString("input_language",v).apply(); private fun setWakeWordEnabled(v:Boolean)=getSharedPreferences("settings",0).edit().putBoolean("wake_word_enabled",v).apply(); private fun saveWakeWord(v:String){if(v.trim().length>=3)getSharedPreferences("settings",0).edit().putString("wake_word",v.trim()).apply()}; private fun requestMic()=requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),10); private fun button(label:String,action:()->Unit)=Button(this).apply{text=label;setOnClickListener{action()}}; private fun openBatterySettings(){runCatching{startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})}.getOrElse{startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}}
}
