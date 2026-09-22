package com.bitwhisper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(private val output: File) {
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(2048)
    private var recorder: AudioRecord? = null
    private var thread: Thread? = null

    fun start() {
        check(recorder == null) { "Already recording" }
        output.parentFile?.mkdirs()
        val audio = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 2)
        recorder = audio
        audio.startRecording()
        thread = Thread {
            FileOutputStream(output).use { stream ->
                val buffer = ByteArray(bufferSize)
                while (recorder === audio) {
                    val count = audio.read(buffer, 0, buffer.size)
                    if (count > 0) stream.write(buffer, 0, count)
                }
            }
        }.also { it.start() }
    }

    fun stop() {
        val audio = recorder ?: return
        recorder = null
        audio.stop()
        audio.release()
        thread?.join(500)
        thread = null
    }
}
