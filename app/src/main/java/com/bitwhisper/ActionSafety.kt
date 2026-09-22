package com.bitwhisper

enum class ActionRisk { SAFE, IMPORTANT }

object ActionSafety {
    private val sensitiveWords = setOf("kirim", "send", "hapus", "delete", "bayar", "transfer", "telepon", "panggil", "publish", "posting")
    fun risk(command: String): ActionRisk = if (sensitiveWords.any { command.lowercase().contains(it) }) ActionRisk.IMPORTANT else ActionRisk.SAFE
}

class ConfirmationManager {
    private var pending: String? = null
    fun request(command: String): String { pending = command; return "Saya perlu konfirmasi sebelum menjalankan: $command. Lanjutkan?" }
    fun pending() = pending
    fun confirm(): String? = pending.also { pending = null }
    fun cancel() { pending = null }
}
