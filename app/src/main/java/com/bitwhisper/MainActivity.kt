package com.bitwhisper

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var content: FrameLayout
    private lateinit var dashboard: LinearLayout
    private lateinit var messageList: LinearLayout
    private val darkMode get() = getSharedPreferences("settings",0).getBoolean("dark_mode", false)
    private val pixelTypeface by lazy { runCatching { Typeface.createFromAsset(assets, "PixelOperator.ttf") }.getOrElse { Typeface.MONOSPACE } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = if (darkMode) Color.rgb(18,18,18) else Color.WHITE
        window.navigationBarColor = if (darkMode) Color.rgb(18,18,18) else Color.WHITE
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(if (darkMode) Color.rgb(18,18,18) else Color.WHITE); clipToPadding = false }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, bars.top, view.paddingRight, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
        val header = LinearLayout(this).apply { setPadding(28, 28, 28, 16); gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "BitWhisper"; textSize = 24f; setTextColor(if (darkMode) Color.WHITE else Color.BLACK) }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("[ SETTINGS ]") { showSettings() })
        root.addView(header)
        content = FrameLayout(this)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        showDashboard()
    }

    private fun showDashboard() {
        content.removeAllViews()
        dashboard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 12, 24, 24) }
        dashboard.addView(TextView(this).apply { text = "Percakapan"; textSize = 28f; setTextColor(if (darkMode) Color.WHITE else Color.BLACK); setPadding(0, 8, 0, 8) })
        val serviceStatus = TextView(this).apply { text = "[OFFLINE] Siap offline"; textSize = 14f; setTextColor(if (darkMode) Color.WHITE else Color.BLACK); setPadding(0, 0, 0, 4) }
        dashboard.addView(serviceStatus)
        dashboard.addView(TextView(this).apply { text = "Tanya apa saja secara offline dengan BitWhisper"; textSize = 15f; setTextColor(if (darkMode) Color.LTGRAY else Color.DKGRAY); setPadding(0, 0, 0, 18) })
        messageList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(4, 8, 4, 8) }
        dashboard.addView(ScrollView(this).apply { addView(messageList) }, LinearLayout.LayoutParams(-1, 0, 1f))
        val input = EditText(this).apply { hint = "Ketik pesan..."; setSingleLine(false); setPadding(18, 12, 18, 12) }
        val send = button("Kirim") { sendChat(input) }
        val mic = button("[ MIC ]") { startForegroundService(Intent(this, VoiceService::class.java)); serviceStatus.text = "[LISTENING] Volume Up"; serviceStatus.setTextColor(if (darkMode) Color.WHITE else Color.BLACK) }
        mic.background = rounded(Color.BLACK, 4f)
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
            text = message; textSize = 16f; typeface = pixelTypeface; setTextColor(if (fromUser) Color.WHITE else if (darkMode) Color.WHITE else Color.BLACK); setPadding(22, 16, 22, 16)
            background = rounded(if (fromUser) Color.BLACK else if (darkMode) Color.rgb(35,35,35) else Color.WHITE, 28f)
        }
        val row = LinearLayout(this).apply { gravity = if (fromUser) Gravity.END else Gravity.START; setPadding(8, 6, 8, 6) }
        row.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { if (fromUser) marginStart = 56 else marginEnd = 56 })
        messageList.addView(row)
    }

    private fun showSettings() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 16, 32, 32); setBackgroundColor(if (darkMode) Color.rgb(18,18,18) else Color.WHITE); isClickable = true; isFocusable = true }
        layout.addView(LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; addView(button("< BACK") { showDashboard() }); addView(TextView(this@MainActivity).apply { text = "PENGATURAN"; textSize = 24f; typeface = pixelTypeface; setTextColor(if (darkMode) Color.WHITE else Color.BLACK); setPadding(16, 0, 0, 0) }) })
        layout.addView(TextView(this).apply { text = "KONFIGURASI OFFLINE - BITWHISPER"; typeface = pixelTypeface; textSize = 12f; setTextColor(if (darkMode) Color.LTGRAY else Color.DKGRAY); setPadding(0, 8, 0, 20) })
        val models = ModelManager(this); val wakeModel = WakeWordModelManager(this); val enrollment = WakeWordEnrollmentStore(this); val status = TextView(this).apply { text = "Whisper: ${if (models.isWhisperReady()) "siap" else "belum"}`nChatbot: ${if (models.isChatReady()) "siap" else "belum"}`nWake word: ${if (enrollment.sampleCount >= 3 || wakeModel.isReady()) "siap" else "belum"}" }
        layout.addView(section("MODEL OFFLINE")); layout.addView(status); val modelStatus = TextView(this); layout.addView(modelStatus); val downloader = ModelDownloader(this)
        layout.addView(button("Unduh model Whisper") { modelStatus.text="Mengunduh Whisper..."; downloader.downloadWhisper({ p -> runOnUiThread { modelStatus.text="Whisper: $p%" } }) { runOnUiThread { modelStatus.text = if(it.isSuccess) "Whisper selesai" else "Whisper gagal" } } })
        layout.addView(button("Unduh model chatbot") { modelStatus.text="Mengunduh chatbot..."; downloader.downloadChat({ p -> runOnUiThread { modelStatus.text="Chatbot: $p%" } }) { runOnUiThread { modelStatus.text = if(it.isSuccess) "Chatbot selesai" else "Chatbot gagal" } } })
        layout.addView(button("Izinkan mikrofon") { requestMic() }); if (android.os.Build.VERSION.SDK_INT >= 33) layout.addView(button("Izinkan notifikasi") { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11) })
        layout.addView(button("Aktifkan Accessibility Service") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }); layout.addView(button("Atur baterai tanpa pembatasan") { openBatterySettings() })
        layout.addView(TextView(this).apply { text="Output jawaban"; textSize=18f; setPadding(0,20,0,8) }); listOf("Text saja" to "text", "Voice saja" to "voice", "Text + Voice" to "both").forEach { layout.addView(button(it.first) { setOutputMode(it.second) }) }
        layout.addView(TextView(this).apply { text="Bahasa input"; textSize=18f; setPadding(0,20,0,8) }); listOf("Otomatis" to "auto", "Indonesia" to "id", "Jawa" to "jv").forEach { layout.addView(button(it.first) { setLanguage(it.second) }) }
        layout.addView(TextView(this).apply { text="Mode chatbot"; textSize=18f; setPadding(0,20,0,8) }); layout.addView(button("Umum") { ChatSettings(this).setMode(ChatMode.GENERAL) }); layout.addView(button("Scientific") { ChatSettings(this).setMode(ChatMode.SCIENTIFIC) })
        layout.addView(TextView(this).apply { text="Wake word"; textSize=18f; setPadding(0,20,0,8) }); val wake = EditText(this).apply { hint="hey bro"; setText(getSharedPreferences("settings",0).getString("wake_word","hey bro")) }; layout.addView(wake); layout.addView(button("Simpan wake word") { saveWakeWord(wake.text.toString()) })
        val enrollStatus=TextView(this).apply{text="Sample hey bro: ${enrollment.sampleCount}/3"}; layout.addView(enrollStatus); layout.addView(button("Rekam sample hey bro") { enrollStatus.text="Ucapkan hey bro sekarang..."; Thread { runCatching { enrollment.addPcmSample(WakeWordEnrollmentStore.recordSample()) }.onSuccess { runOnUiThread { enrollStatus.text="Sample hey bro: ${enrollment.sampleCount}/3" } }.onFailure { runOnUiThread { enrollStatus.text="Gagal merekam sample" } } }.start() }); layout.addView(button("Hapus sample wake word") { enrollment.clear(); enrollStatus.text="Sample hey bro: 0/3" })
        layout.addView(button("< BACK") { showDashboard() })
        content.addView(ScrollView(this).apply { addView(layout) })
    }

    private fun optionLabel(label: String, selected: Boolean): String {
        return if (selected) "[X] $label" else "[ ] $label"
    }

    private fun section(title:String)=TextView(this).apply{text=title;typeface=pixelTypeface;textSize=13f;setTextColor(if (darkMode) Color.WHITE else Color.BLACK);setPadding(0,20,0,8)}
    private fun toggleDarkMode(){ val dark=!darkMode; getSharedPreferences("settings",0).edit().putBoolean("dark_mode",dark).apply(); recreate() }
    private fun rounded(color:Int, radius:Float)=GradientDrawable().apply{setColor(color);cornerRadius=radius;setStroke(2, Color.BLACK)}
    private fun setOutputMode(v:String)=getSharedPreferences("settings",0).edit().putString("output_mode",v).apply(); private fun setLanguage(v:String)=getSharedPreferences("settings",0).edit().putString("input_language",v).apply(); private fun setWakeWordEnabled(v:Boolean)=getSharedPreferences("settings",0).edit().putBoolean("wake_word_enabled",v).apply(); private fun saveWakeWord(v:String){if(v.trim().length>=3)getSharedPreferences("settings",0).edit().putString("wake_word",v.trim()).apply()}; private fun requestMic()=requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),10); private fun button(label:String,action:()->Unit)=Button(this).apply{text=label;typeface=pixelTypeface;setTextColor(if (darkMode) Color.WHITE else Color.BLACK);background=rounded(if (darkMode) Color.rgb(35,35,35) else Color.WHITE,4f);setAllCaps(false);minHeight=52;contentDescription=label;setPadding(16,8,16,8);setOnClickListener{action()}}; private fun openBatterySettings(){runCatching{startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply{data=Uri.parse("package:$packageName")})}.getOrElse{startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))}}
}




