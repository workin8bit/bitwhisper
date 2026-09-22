package com.bitwhisper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class WakeWordAudioMonitor(private val onFrame: (ShortArray) -> Boolean) {
    private val sampleRate = 16_000
    private val frameSamples = 1_600 // 100 ms
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(frameSamples * 2)
    @Volatile private var running = false
    private var recorder: AudioRecord? = null
    private var worker: Thread? = null

    fun start() {
        if (running) return
        val audio = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
        recorder = audio
        running = true
        audio.startRecording()
        worker = Thread {
            val bytes = ShortArray(frameSamples)
            try {
                while (running) {
                    val read = audio.read(bytes, 0, bytes.size)
                    if (read > 0 && onFrame(bytes.copyOf(read))) break
                }
            } finally {
                running = false
                audio.stop()
                audio.release()
                recorder = null
            }
        }.also { it.start() }
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
        recorder = null
    }
}
