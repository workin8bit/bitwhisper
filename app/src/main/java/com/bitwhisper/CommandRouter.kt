package com.bitwhisper

/** Deterministic, offline intents. The local LLM can be added behind this interface later. */
class CommandRouter {
    fun route(input: String): IntentResult {
        val text = input.trim().lowercase()
        return when {
            text.startsWith("buka ") || text.startsWith("bukak ") -> IntentResult.OpenApp(input.substringAfter(' ').trim())
            text.startsWith("buat catatan ") || text.startsWith("gawe cathetan ") -> IntentResult.CreateNote(input.substringAfter(' ').trim())
            text.startsWith("setel timer ") || text.startsWith("pasang timer ") || text.startsWith("gawe timer ") -> IntentResult.Timer(input.substringAfterLast(' ').toIntOrNull() ?: 0)
            text == "nyalakan senter" || text == "uripna senter" || text == "matikan senter" || text == "pateni senter" -> IntentResult.Flashlight(text.startsWith("nyalakan") || text.startsWith("uripna"))
            else -> IntentResult.Dictation(input)
        }
    }
}

sealed interface IntentResult {
    data class OpenApp(val name: String) : IntentResult
    data class CreateNote(val body: String) : IntentResult
    data class Timer(val minutes: Int) : IntentResult
    data class Flashlight(val enabled: Boolean) : IntentResult
    data class Dictation(val text: String) : IntentResult
}
