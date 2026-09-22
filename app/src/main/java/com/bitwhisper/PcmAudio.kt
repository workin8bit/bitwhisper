package com.bitwhisper

import java.io.File
import java.io.FileOutputStream

object PcmAudio {
    /** Wraps 16-bit mono PCM at 16 kHz in a WAV container for native engines/tools. */
    fun toWav(pcm: File, wav: File, sampleRate: Int = 16_000) {
        val dataLength = pcm.length().toInt()
        FileOutputStream(wav).use { out ->
            fun intLE(value: Int) = out.write(byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte()))
            fun shortLE(value: Int) = out.write(byteArrayOf(value.toByte(), (value shr 8).toByte()))
            out.write("RIFF".toByteArray()); intLE(dataLength + 36); out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); intLE(16); shortLE(1); shortLE(1)
            intLE(sampleRate); intLE(sampleRate * 2); shortLE(2); shortLE(16)
            out.write("data".toByteArray()); intLE(dataLength)
            pcm.inputStream().use { it.copyTo(out) }
        }
    }
}
