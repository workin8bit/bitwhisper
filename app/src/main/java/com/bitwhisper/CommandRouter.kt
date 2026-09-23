package com.bitwhisper

/** Deterministic, offline intents. The local LLM can be added behind this interface later. */
class CommandRouter {
    fun route(input: String): IntentResult {
        val text = normalizeCommand(input)

        // Fast offline corrections for frequent Whisper phonetic mistakes.
        // The local layer runs before any future Qwen intent resolver.
        

        return when {
            text == "buka pengaturan" || text == "buka settings" -> IntentResult.OpenApp("__settings__")
            text in setOf("jawab panggilan", "angkat panggilan", "angkat") -> IntentResult.CallControl("answer")
            text in setOf("tolak panggilan", "tolak", "tutup panggilan") -> IntentResult.CallControl("reject")
            text in setOf("nyalakan loudspeaker", "aktifkan speaker", "pakai speaker") -> IntentResult.CallControl("speaker_on")
            text in setOf("matikan loudspeaker", "matikan speaker", "kembali ke earpiece") -> IntentResult.CallControl("speaker_off")
            text.startsWith("buka ") || text.startsWith("bukak ") -> IntentResult.OpenApp(input.substringAfter(' ').trim())
            text.startsWith("buat catatan ") || text.startsWith("gawe cathetan ") -> IntentResult.CreateNote(input.substringAfter(' ').trim())
            text.startsWith("setel timer ") || text.startsWith("pasang timer ") || text.startsWith("gawe timer ") -> IntentResult.Timer(input.substringAfterLast(' ').toIntOrNull() ?: 0)
            text == "nyalakan senter" || text == "uripna senter" || text == "matikan senter" || text == "pateni senter" -> IntentResult.Flashlight(text.startsWith("nyalakan") || text.startsWith("uripna"))
            else -> IntentResult.Dictation(input)
        }
    }

    private fun normalizeCommand(input: String): String {
        var text = input.trim().lowercase().replace(Regex("\\s+"), " ")
        val replacements = mapOf(
            "puka " to "buka ",
            "bukka " to "buka ",
            "bukak " to "buka ",
            "tulis " to "ketik "
        )
        replacements.forEach { (wrong, right) -> if (text.startsWith(wrong)) text = right + text.removePrefix(wrong) }
        return text
    }
}

sealed interface IntentResult {
    data class OpenApp(val name: String) : IntentResult
    data class CallControl(val action: String) : IntentResult
    data class CreateNote(val body: String) : IntentResult
    data class Timer(val minutes: Int) : IntentResult
    data class Flashlight(val enabled: Boolean) : IntentResult
    data class Dictation(val text: String) : IntentResult
}
