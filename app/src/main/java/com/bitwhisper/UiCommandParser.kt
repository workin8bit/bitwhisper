package com.bitwhisper

sealed interface UiCommand {
    data class Click(val label: String) : UiCommand
    data class Fill(val target: String, val value: String) : UiCommand
    data object ScrollDown : UiCommand
    data object ScrollUp : UiCommand
}

object UiCommandParser {
    fun parse(input: String): UiCommand? {
        val text = input.trim()
        val lower = text.lowercase()
        return when {
            lower.startsWith("klik tombol ") -> UiCommand.Click(text.substring(12).trim())
            lower.startsWith("tekan tombol ") -> UiCommand.Click(text.substring(13).trim())
            lower.startsWith("ketik ") -> UiCommand.Fill("", text.substring(6).trim())
            lower.startsWith("tulis ") -> UiCommand.Fill("", text.substring(6).trim())
            lower.startsWith("isi ") && lower.contains(" dengan ") -> {
                val parts = text.substring(4).split(" dengan ", limit = 2)
                if (parts.size == 2) UiCommand.Fill(parts[0].trim(), parts[1].trim()) else null
            }
            lower == "scroll ke bawah" || lower == "gulir ke bawah" -> UiCommand.ScrollDown
            lower == "scroll ke atas" || lower == "gulir ke atas" -> UiCommand.ScrollUp
            else -> null
        }
    }
}
