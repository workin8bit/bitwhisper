package com.bitwhisper

/** Low-power wake-word boundary. The TFLite/openWakeWord implementation plugs in here. */
interface WakeWordEngine {
    fun start(onDetected: () -> Unit)
    fun stop()
    fun isAvailable(): Boolean
}

class LocalWakeWordEngine : WakeWordEngine {
    private var running = false
    override fun start(onDetected: () -> Unit) { running = true /* TODO: native keyword model */ }
    override fun stop() { running = false }
    override fun isAvailable() = false
}
