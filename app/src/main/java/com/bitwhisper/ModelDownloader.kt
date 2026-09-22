package com.bitwhisper

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloader(context: Context) {
    private val manager = ModelManager(context)
    fun downloadWhisper(onProgress: (Int) -> Unit, onDone: (Result<File>) -> Unit) = download(ModelLinks.WHISPER, manager.whisperModel, onProgress, onDone)
    fun downloadChat(onProgress: (Int) -> Unit, onDone: (Result<File>) -> Unit) = download(ModelLinks.CHAT, manager.chatModel, onProgress, onDone)

    private fun download(url: String, target: File, progress: (Int) -> Unit, done: (Result<File>) -> Unit) {
        Thread {
            runCatching {
                target.parentFile?.mkdirs()
                val temp = File(target.parentFile, target.name + ".part")
                var offset = if (temp.exists()) temp.length() else 0L
                var connection = open(url, offset)
                if (offset > 0 && connection.responseCode == HttpURLConnection.HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                    connection.disconnect(); temp.delete(); offset = 0; connection = open(url, 0)
                }
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                val append = offset > 0 && connection.responseCode == HttpURLConnection.HTTP_PARTIAL
                val total = if (append) offset + connection.contentLengthLong else connection.contentLengthLong
                connection.inputStream.use { input -> temp.outputStream().buffered().use { output ->
                    if (append) { output.close(); temp.outputStream().buffered().use { resumed -> input.copyTo(resumed) } }
                    else input.copyTo(output)
                } }
                if (total > 0) progress(100)
                if (!temp.renameTo(target)) error("Tidak dapat menyimpan model")
                target
            }.onSuccess { done(Result.success(it)) }.onFailure { done(Result.failure(it)) }
        }.start()
    }

    private fun open(url: String, offset: Long): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000; readTimeout = 60_000; instanceFollowRedirects = true
        setRequestProperty("User-Agent", "BitWhisper/0.1")
        if (offset > 0) setRequestProperty("Range", "bytes=$offset-")
        connect()
    }
}
