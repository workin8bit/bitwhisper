package com.bitwhisper

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout
    private lateinit var dashboard: LinearLayout
    private lateinit var messageList: LinearLayout

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
        val serviceStatus = TextView(this).apply { text = "● Siap offline"; textSize = 14f; setTextColor(Color.rgb(30,145,80)); setPadding(0, 0, 0, 4) }
        dashboard.addView(serviceStatus)
        dashboard.addView(TextView(this).apply { text = "Tanya apa saja secara offline dengan BitWhisper"; textSize = 15f; setTextColor(Color.DKGRAY); setPadding(0, 0, 0, 18) })
        messageList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 8, 4, 8) }
        dashboard.addView(ScrollView(this).apply { addView(messageList) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val input = EditText(this).apply { hint = "Ketik pesan..."; setSingleLine(false); setPadding(18, 12, 18, 12) }
        val send = button("Kirim") { sendChat(input) }
        val mic = button("🎙") { startForegroundService(Intent(this, VoiceService::class.java)); serviceStatus.text = "● Mendengarkan Volume Up"; serviceStatus.setTextColor(Color.rgb(210,80,55)) }
        mic.background = rounded(Color.rgb(35,95,180), 22)
        mic.setTextColor(Color.WHITE)
        val composer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 12, 0, 0) }
        composer.addView(mic); composer.addView(input, LinearLayout.LayoutParams(0, -2, 1f)); composer.addView(send)
        dashboard.addView(composer)
        content.addView(ScrollView(this).apply { addView(dashboard) })
        refreshHistory()
    }

    private fun sendChat(input: EditText) {
        val prompt = input.text.toString().trim(); if (prompt.isEmpty()) return
        input.text.clear(); refreshHistory(); addBubble("BitWhisper sedang berpikir...", false)
        Thread {
            val answer = LocalChatEngine(this).respond(prompt, ConversationStore(this).all().takeLast(4).map { it.user to it.assistant }, ChatSettings(this).mode())
            ConversationStore(this).add(prompt, answer)
            runOnUiThread { refreshHistory() }
        }.start()
    }

    private fun refreshHistory() {
        if (!::messageList.isInitialized) return
        messageList.removeAllViews()
        val entries = ConversationStore(this).all().takeLast(30)
        if (entries.isEmpty()) addBubble("Belum ada percakapan.\n\nMulai dengan pertanyaan di bawah.", false)
        entries.forEach { addBubble(it.user, true); addBubble(it.assistant, false) }
    }

    private fun addBubble(message: String, fromUser: Boolean) {
        if (!::messageList.isInitialized) return
        val bubble = TextView(this).apply {
            text = message; textSize = 16f; setTextColor(if (fromUser) Color.WHITE else Color.rgb(35,40,50)); setPadding(22, 16, 22, 16)
            background = rounded(if (fromUser) Color.rgb(35,95,180) else Color.WHITE, 28f)
        }
        val row = LinearLayout(this).apply { gravity = if (fromUser) Gravity.END else Gravity.START; setPadding(8, 6, 8, 6) }
        row.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { if (fromUser) marginStart = 56 else marginEnd = 56 })
        messageList.addView(row)
    }

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
        layout.addView(button("Aktifkan wake word") { setWakeWordEnabled(true) }); layout.addView(button("Matikan wake word") { setWakeWordEnabled(false) }); layout.addView(button("Mulai BitWhisper") { startForegroundService(Intent(this,VoiceService::class.java)) }); layout.addView(button("Mode gelap / terang") { toggleDarkMode() }); layout.addView(button("Kembali ke dashboard") { showDashboard() })
        content.addView(ScrollView(this).apply { addView(layout) })
    }

    private fun toggleDarkMode(){ val dark=!getSharedPreferences("settings",0).getBoolean("dark_mode",false); getSharedPreferences("settings",0).edit().putBoolean("dark_mode",dark).apply(); window.decorView.setBackgroundColor(if(dark) Color.rgb(25,28,35) else Color.rgb(248,249,252)); Toast.makeText(this,if(dark) "Mode gelap aktif" else "Mode terang aktif",Toast.LENGTH_SHORT).show() }
    private fun rounded(color:Int, radius:Float)=GradientDrawable().apply{setColor(color);cornerRadius=radius}
    private fun setOutputMode(v:String)=getSharedPreferences("settings",0).edit().putString("output_mode",v).apply(); private fun setLanguage(v:String)=getSharedPreferences("settings",0).edit().putString("input_language",v).apply(); private fun setWakeWordEnabled(v:Boolean)=getSharedPreferences("settings",0).edit().putBoolean("wake_word_enabled",v).apply(); private fun saveWakeWord(v:String){if(v.trim().length>=3)getSharedPreferences("settings",0).edit().putString("wake_word",v.trim()).apply()}; private fun requestMic()=requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),10); private fun button(label:String,action:()->Unit)=Button(this).apply{text=label;setOnClickListener{action()}}; private fun openBatterySettings(){runCatching{startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})}.getOrElse{startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}}
}
