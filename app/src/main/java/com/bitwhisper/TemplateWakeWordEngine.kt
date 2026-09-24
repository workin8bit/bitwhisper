package com.bitwhisper

import android.content.Context

class TemplateWakeWordEngine(context: Context) : WakeWordEngine {
    private val store = WakeWordEnrollmentStore(context)
    private var monitor: WakeWordAudioMonitor? = null
    private var callback: (() -> Unit)? = null
    private var lastDetected = 0L
    override fun start(onDetected: () -> Unit) {
        if (!store.isReady() || monitor != null) return
        callback = onDetected
        monitor = WakeWordAudioMonitor { frame ->
            val score = store.similarity(frame)
            if (score >= 0.82f && System.currentTimeMillis() - lastDetected > 3_000) { lastDetected = System.currentTimeMillis(); callback?.invoke(); true } else false
        }.also { it.start() }
    }
    override fun stop() { monitor?.stop(); monitor = null; callback = null }
    override fun isAvailable() = store.isReady()
}
