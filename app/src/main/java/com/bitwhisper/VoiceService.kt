package com.bitwhisper

import android.app.*
import android.content.*
import android.os.IBinder
import android.util.Log
import java.io.File

class VoiceService : Service() {
    private val commandRouter = CommandRouter()
    private val transcriber: TranscriptionEngine by lazy { LocalWhisperEngine(this) }
    private val commandExecutor by lazy { CommandExecutor(this) }
    private val speaker by lazy { ResponseSpeaker(this) }
    private val history by lazy { ConversationStore(this) }
    private val confirmations = ConfirmationManager()
    private val planner = AgentPlanner()
    private val memory by lazy { MemoryStore(this) }
    private val skills by lazy { SkillStore(this) }
    private var wakeEngine: WakeWordEngine? = null
    private var recorder: AudioRecorder? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BitWhisperAccessibilityService.ACTION_RECORD_START -> startRecording()
                BitWhisperAccessibilityService.ACTION_RECORD_STOP -> stopRecording()
                BitWhisperNotificationListener.ACTION_INCOMING_CALL -> {
                    val label = intent.getStringExtra("label").orEmpty()
                    speaker.speak("Ada panggilan masuk${if (label.isNotBlank()) ": $label" else ""}. Bilang angkat atau tolak.")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val filter = IntentFilter().apply {
            addAction(BitWhisperAccessibilityService.ACTION_RECORD_START)
            addAction(BitWhisperAccessibilityService.ACTION_RECORD_STOP)
            addAction(BitWhisperNotificationListener.ACTION_INCOMING_CALL)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(receiver, filter)
        }
        startForeground(1, notification())
        // Let TTS finish initializing, then confirm that voice input is ready.
        android.os.Handler(mainLooper).postDelayed({ speaker.announceReady() }, 700L)
        startWakeWordIfEnabled()
    }

    private fun startWakeWordIfEnabled() {
        val settings = WakeWordSettings(this)
        if (!settings.enabled) return
        val engine = OnnxWakeWordEngine(this)
        if (!engine.isAvailable()) return
        wakeEngine = engine
        runCatching { engine.start { startRecording() } }.onFailure { wakeEngine = null }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (recorder != null) return
        recorder = AudioRecorder(File(cacheDir, "last-recording.pcm")).also { it.start() }
    }

    private fun stopRecording() {
        val active = recorder ?: return
        recorder = null
        active.stop()
        Thread {
            // Give the short post-recording inference enough CPU priority to minimize
            // the gap between releasing Volume Up and inserting the text.
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)
            val pcm = File(cacheDir, "last-recording.pcm")
            val transcript = transcriber.transcribe(pcm).trim()
            Log.d("BitWhisperVoice", "Transcription length=${transcript.length}")
            if (transcript.isNotEmpty()) {
                onTranscript(transcript)
            }
        }.start()
    }

    override fun onDestroy() {
        wakeEngine?.stop()
        wakeEngine = null
        speaker.close()
        recorder?.stop()
        recorder = null
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    /** Entry point shared by Whisper and tests. */
    fun onTranscript(text: String): IntentResult {
        val normalized = text.trim().lowercase()
        confirmations.pending()?.let { pending ->
            if (normalized in setOf("ya", "iya", "lanjut", "kirim", "confirm")) {
                confirmations.confirm()
                val pendingUi = UiCommandParser.parse(pending)
                if (pendingUi != null) {
                    val response = executeConfirmedUi(pendingUi)
                    history.add(text, response)
                    speaker.speak(response)
                    return IntentResult.Dictation(response)
                }
                val result = commandRouter.route(pending)
                val response = commandExecutor.execute(result)
                history.add(text, response)
                speaker.speak(response)
                return result
            }
            if (normalized in setOf("tidak", "batal", "cancel", "jangan")) {
                confirmations.cancel()
                val response = "Baik, dibatalkan."
                history.add(text, response)
                speaker.speak(response)
                return IntentResult.Dictation(response)
            }
        }
        if (normalized == "siapa yang menelepon" || normalized == "ada panggilan" || normalized == "baca layar" || normalized == "baca pesan" || normalized == "baca pesan terakhir") {
            val service = BitWhisperAccessibilityService.active
            val screenText = service?.let { ScreenReader(it).dump().trim() }.orEmpty()
            val notification = getSharedPreferences(BitWhisperNotificationListener.PREFS, MODE_PRIVATE).getString("last_notification", "").orEmpty()
            val response = when {
                normalized == "siapa yang menelepon" || normalized == "ada panggilan" ->
                    if (notification.isNotBlank()) notification else "Aku belum melihat panggilan masuk."
                screenText.isNotBlank() -> screenText.lines().takeLast(12).joinToString(" ")
                notification.isNotBlank() -> notification
                else -> "Aku belum bisa melihat pesan atau teks sekarang."
            }
            history.add(text, response)
            speaker.speak(response)
            return IntentResult.Dictation(response)
        }
        val remember = Regex("ingat(?:i)?\\s+(.+?)\\s+(?:adalah|itu)\\s+(.+)", RegexOption.IGNORE_CASE).find(text)
        if (remember != null) {
            memory.remember(remember.groupValues[1], remember.groupValues[2])
            val response = "Baik, saya ingat ${remember.groupValues[1]}."
            history.add(text, response); speaker.speak(response)
            return IntentResult.Dictation(response)
        }
        val recall = Regex("(?:siapa|apa)\\s+(?:nama|nilai)\\s+(.+?)\\??$", RegexOption.IGNORE_CASE).find(text)
        if (recall != null) {
            val response = memory.recall(recall.groupValues[1]) ?: "Saya belum memiliki informasi itu."
            history.add(text, response); speaker.speak(response)
            return IntentResult.Dictation(response)
        }
        skills.find(text)?.let { skill ->
            val response = "Menjalankan skill ${skill.name}."
            history.add(text, response); speaker.speak(response)
            return IntentResult.Dictation(response)
        }
        val plan = planner.plan(text)
        if (plan.size > 1) {
            val response = "Saya menyiapkan ${plan.size} langkah untuk perintah ini."
            history.add(text, response); speaker.speak(response)
            return IntentResult.Dictation(response)
        }
        val uiCommand = UiCommandParser.parse(text)
        if (uiCommand != null) {
            val response = executeUiCommand(uiCommand)
            history.add(text, response)
            speaker.speak(response)
            return IntentResult.Dictation(response)
        }
        val result = commandRouter.route(text)
        if (ActionSafety.risk(text) == ActionRisk.IMPORTANT) {
            val response = confirmations.request(text)
            history.add(text, response)
            speaker.speak(response)
            return result
        }
        val response = commandExecutor.execute(result)
        if (result is IntentResult.Dictation) deliverText(result.text)
        history.add(text, response)
        speaker.speak(response)
        return result
    }

    private fun executeConfirmedUi(command: UiCommand): String {
        val service = BitWhisperAccessibilityService.active ?: return "Accessibility Service belum aktif."
        val executor = UiActionExecutor(service)
        return if (command is UiCommand.Click && executor.clickConfirmed(command.label)) "Tombol ${command.label} ditekan." else "Aksi tidak dapat dijalankan."
    }

    private fun executeUiCommand(command: UiCommand): String {
        val service = BitWhisperAccessibilityService.active ?: return "Accessibility Service belum aktif."
        val executor = UiActionExecutor(service)
        return when (command) {
            is UiCommand.Click -> if (ActionSafety.risk(command.label) == ActionRisk.IMPORTANT) confirmations.request("tekan tombol ${command.label}") else if (executor.click(command.label)) "Tombol ${command.label} ditekan." else "Tombol tidak ditemukan."
            is UiCommand.Fill -> if (executor.setText(command.target, command.value)) "Kolom telah diisi." else "Kolom tidak ditemukan."
            UiCommand.ScrollDown -> if (executor.scrollForward()) "Layar digulir ke bawah." else "Tidak dapat menggulir layar."
            UiCommand.ScrollUp -> if (executor.scrollBackward()) "Layar digulir ke atas." else "Tidak dapat menggulir layar."
        }
    }

    private fun deliverText(text: String) {
        if (BitWhisperAccessibilityService.active?.insertText(text) == true) return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("BitWhisper", text))
    }

    private fun createChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("voice", "BitWhisper", NotificationManager.IMPORTANCE_LOW)
        )
    }
    private fun notification() = Notification.Builder(this, "voice")
        .setContentTitle("BitWhisper aktif")
        .setContentText("Tahan Volume Up untuk merekam")
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .build()
}
