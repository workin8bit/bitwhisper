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
                val connection = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 20_000; readTimeout = 60_000; requestMethod = "GET" }
                connection.connect()
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                val total = connection.contentLengthLong
                connection.inputStream.use { input -> temp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024); var received = 0L; var read: Int
                    while (input.read(buffer).also { read = it } != -1) { output.write(buffer, 0, read); received += read; if (total > 0) progress((received * 100 / total).toInt()) }
                } }
                if (!temp.renameTo(target)) error("Tidak dapat menyimpan model")
                target
            }.onSuccess { done(Result.success(it)) }.onFailure { done(Result.failure(it)) }
        }.start()
    }
}
