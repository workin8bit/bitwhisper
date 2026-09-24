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
            text.startsWith("buka ") || text.startsWith("bukak ") -> IntentResult.OpenApp(text.substringAfter(' ').trim())
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
            "lets up" to "whatsapp",
            "let s up" to "whatsapp",
            "let sup" to "whatsapp",
            "bukak " to "buka ",
            "tulis " to "ketik "
        )
        replacements.forEach { (wrong, right) -> if (text.startsWith(wrong)) text = right + text.removePrefix(wrong) }
        text = text.replace("lets up", "whatsapp")
    .replace("let s up", "whatsapp")
    .replace("let sup", "whatsapp")
    .replace("wads app", "whatsapp")
    .replace("wats app", "whatsapp")
    .replace("what s app", "whatsapp")
    .replace("watsapp", "whatsapp")
    .replace("wacap", "whatsapp")
    .replace("wa sap", "whatsapp")
    .replace("wasap", "whatsapp")
    .replace(Regex("\\bwa\\b"), "whatsapp")

PowerShell

git status --short
Harus terlihat:

text

 M app/src/main/java/com/bitwhisper/CommandRouter.kt
Kemudian:

PowerShell

git add app/src/main/java/com/bitwhisper/CommandRouter.kt
git commit -m "fix: normalize common Whisper WhatsApp misrecognitions"
git push origin arena/01a0c7e5-bitwhisper
File font tambahan
File berikut boleh dibiarkan untracked dan tidak perlu di-add:

text

.gradle/
.kotlin/
app/.cxx/
app/build/
build-error.log
crash.log
ggml-tiny.bin
kotlin-error.log
Untuk font, cukup commit:

text

app/src/main/assets/PixelOperator.ttf
File font Pixel Operator lain dan LICENSE.txt tidak perlu di-add kecuali memang ingin semua varian font dikirim ke APK.


6s | 2 minutes ago
16.20


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
