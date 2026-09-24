package com.bitwhisper

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.abs
import kotlin.math.sqrt

class WakeWordEnrollmentStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("wake_word_enrollment", Context.MODE_PRIVATE)
    val sampleCount: Int get() = prefs.getInt("count", 0)
    fun isReady() = sampleCount >= 3

    fun addPcmSample(samples: ShortArray) {
        val feature = feature(samples)
        val encoded = Base64.getEncoder().encodeToString(ByteBuffer.allocate(feature.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { feature.forEach { putFloat(it) } }.array())
        prefs.edit().putString("sample_$sampleCount", encoded).putInt("count", sampleCount + 1).apply()
    }

    fun similarity(samples: ShortArray): Float {
        if (!isReady()) return 0f
        val current = feature(samples)
        var total = 0f
        for (i in 0 until sampleCount) {
            val bytes = Base64.getDecoder().decode(prefs.getString("sample_$i", ""))
            val stored = FloatArray(bytes.size / 4)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(stored)
            var dot = 0f; var a = 0f; var b = 0f
            current.indices.forEach { j -> dot += current[j] * stored[j]; a += current[j] * current[j]; b += stored[j] * stored[j] }
            total += if (a > 0 && b > 0) dot / sqrt(a * b) else 0f
        }
        return total / sampleCount
    }

    fun clear() { prefs.edit().clear().apply() }

    private fun feature(samples: ShortArray): FloatArray {
        val bins = 32; val out = FloatArray(bins); val size = (samples.size / bins).coerceAtLeast(1)
        for (i in samples.indices) out[(i / size).coerceAtMost(bins - 1)] += abs(samples[i].toFloat())
        val norm = sqrt(out.sumOf { (it * it).toDouble() }).toFloat().coerceAtLeast(1f)
        return out.map { it / norm }.toFloatArray()
    }

    companion object {
        fun recordSample(): ShortArray {
            val n = 16_000 * 2
            val buffer = ShortArray(n)
            val record = AudioRecord(MediaRecorder.AudioSource.MIC, 16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, n * 2)
            record.startRecording(); var offset = 0
            while (offset < n) { val read = record.read(buffer, offset, n - offset); if (read <= 0) break; offset += read }
            record.stop(); record.release(); return buffer.copyOf(offset)
        }
    }
}
