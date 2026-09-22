package com.bitwhisper

import android.content.Context
import java.io.File

class WakeWordModelManager(context: Context) {
    private val directory = File(context.filesDir, "models/wakeword")
    val modelFile = File(directory, "wakeword.onnx")
    val metadataFile = File(directory, "model_info.json")
    fun isReady() = modelFile.exists() && modelFile.length() > 10_000 && metadataFile.exists()
    fun ensureDirectory() = directory.mkdirs()
}
