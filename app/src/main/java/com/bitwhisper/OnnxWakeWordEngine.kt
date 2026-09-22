package com.bitwhisper

import android.content.Context
import ai.onnxruntime.OrtEnvironment

class OnnxWakeWordEngine(context: Context) : WakeWordEngine {
    private val model = WakeWordModelManager(context)
    private val environment = OrtEnvironment.getEnvironment()
    private var running = false
    private var detected: (() -> Unit)? = null
    private var monitor: WakeWordAudioMonitor? = null

    override fun start(onDetected: () -> Unit) {
        check(model.isReady()) { "Wake-word ONNX model is missing" }
        if (running) return
        running = true
        detected = onDetected
        monitor = WakeWordAudioMonitor { frame -> acceptFrame(frame) }.also { it.start() }
    }
    override fun stop() {
        running = false
        monitor?.stop()
        monitor = null
        detected = null
    }
    /** Returns true when a frame crosses the model threshold. Tensor mapping is model-specific. */
    private fun acceptFrame(frame: ShortArray): Boolean {
        if (!running || frame.isEmpty()) return false
        // TODO: convert PCM frame to the selected model's input tensor and run OrtSession.
        return false
    }
    override fun isAvailable() = model.isReady()
    fun runtimeAvailable() = environment != null
}
