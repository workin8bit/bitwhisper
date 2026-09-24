package com.bitwhisper

class CommandRouter {
    fun route(input: String): IntentResult {
        val text = normalizeCommand(input)

        return when {
            text == "buka pengaturan" || text == "buka settings" ->
                IntentResult.OpenApp("__settings__")

            text in setOf("jawab panggilan", "angkat panggilan", "angkat") ->
                IntentResult.CallControl("answer")

            text in setOf("tolak panggilan", "tolak", "tutup panggilan") ->
                IntentResult.CallControl("reject")

            text in setOf(
                "nyalakan loudspeaker",
                "aktifkan speaker",
                "pakai speaker"
            ) -> IntentResult.CallControl("speaker_on")

            text in setOf(
                "matikan loudspeaker",
                "matikan speaker",
                "kembali ke earpiece"
            ) -> IntentResult.CallControl("speaker_off")

            text.startsWith("buka ") ->
                IntentResult.OpenApp(text.substringAfter(" ").trim())

            text.startsWith("buat catatan ") ->
                IntentResult.CreateNote(input.substringAfter(" ").trim())

            text.startsWith("setel timer ") ->
                IntentResult.Timer(
                    text.substringAfterLast(" ").toIntOrNull() ?: 0
                )

            text == "nyalakan senter" ||
                text == "uripna senter" ||
                text == "matikan senter" ||
                text == "pateni senter" ->
                IntentResult.Flashlight(
                    text.startsWith("nyalakan") || text.startsWith("uripna")
                )

            else -> IntentResult.Dictation(input)
        }
    }

    private fun normalizeCommand(input: String): String {
        var text = input
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

        if (text.startsWith("puka ")) {
            text = "buka " + text.removePrefix("puka ")
        }

        if (text.startsWith("bukak ")) {
            text = "buka " + text.removePrefix("bukak ")
        }

        if (text.startsWith("buka wa")) {
            text = "buka whatsapp"
        }

        text = text
            .replace("wads app", "whatsapp")
            .replace("wats app", "whatsapp")
            .replace("what s app", "whatsapp")
            .replace("watsapp", "whatsapp")
            .replace("wacap", "whatsapp")
            .replace("wa sap", "whatsapp")
            .replace("wasap", "whatsapp")
            .replace("lets up", "whatsapp")
            .replace("let s up", "whatsapp")
            .replace("let sup", "whatsapp")
            .replace(Regex("\\bwa\\b"), "whatsapp")

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