package com.bitwhisper

/**
 * Router perintah offline.
 *
 * Pipeline:
 * Whisper
 * -> normalisasi typo lokal
 * -> fuzzy correction ringan
 * -> intent deterministik
 * -> executor
 *
 * Qwen hanya dipakai sebagai fallback jika router lokal tidak mengenali
 * perintah tersebut.
 */
class CommandRouter {

    fun route(input: String): IntentResult {
        val text = normalizeCommand(input)

        return when {
            text == "buka pengaturan" ||
                text == "buka settings" -> {
                IntentResult.OpenApp("__settings__")
            }

            text in setOf(
                "jawab panggilan",
                "angkat panggilan",
                "angkat"
            ) -> {
                IntentResult.CallControl("answer")
            }

            text in setOf(
                "tolak panggilan",
                "tolak",
                "tutup panggilan"
            ) -> {
                IntentResult.CallControl("reject")
            }

            text in setOf(
                "nyalakan loudspeaker",
                "aktifkan speaker",
                "pakai speaker",
                "nyalakan speaker"
            ) -> {
                IntentResult.CallControl("speaker_on")
            }

            text in setOf(
                "matikan loudspeaker",
                "matikan speaker",
                "kembali ke earpiece"
            ) -> {
                IntentResult.CallControl("speaker_off")
            }

            text.startsWith("buka ") -> {
                IntentResult.OpenApp(
                    text.substringAfter(" ").trim()
                )
            }

            text.startsWith("buat catatan ") -> {
                IntentResult.CreateNote(
                    input.substringAfter(" ").trim()
                )
            }

            text.startsWith("ketik ") -> {
                IntentResult.Dictation(
                    input.substringAfter(" ").trim()
                )
            }

            text.startsWith("setel timer ") ||
                text.startsWith("pasang timer ") ||
                text.startsWith("buat timer ") -> {
                IntentResult.Timer(
                    extractLastNumber(text)
                )
            }

            text == "nyalakan senter" ||
                text == "hidupkan senter" ||
                text == "uripna senter" -> {
                IntentResult.Flashlight(true)
            }

            text == "matikan senter" ||
                text == "padamkan senter" ||
                text == "pateni senter" -> {
                IntentResult.Flashlight(false)
            }

            else -> {
                IntentResult.Dictation(input)
            }
        }
    }

    private fun normalizeCommand(input: String): String {
        var text = input
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")

        if (text.isBlank()) {
            return text
        }

        /*
         * Koreksi frasa yang sering salah didengar Whisper.
         */
        val phraseReplacements = listOf(
            "what s app" to "whatsapp",
            "whats app" to "whatsapp",
            "wats app" to "whatsapp",
            "wads app" to "whatsapp",
            "lets up" to "whatsapp",
            "let s up" to "whatsapp",
            "let sup" to "whatsapp",
            "wa sap" to "whatsapp",
            "watsapp" to "whatsapp",
            "wacap" to "whatsapp",
            "wasap" to "whatsapp",
            "you tube" to "youtube",
            "you too" to "youtube",
            "google crom" to "google chrome",
            "buka wa" to "buka whatsapp",
            "bukak wa" to "buka whatsapp",
            "puka wa" to "buka whatsapp"
        )

        phraseReplacements.forEach { (wrong, correct) ->
            text = text.replace(wrong, correct)
        }

        /*
         * Koreksi awalan kata kerja.
         */
        val prefixReplacements = listOf(
            "puka " to "buka ",
            "bukka " to "buka ",
            "bukak " to "buka ",
            "bukak " to "buka ",
            "tulis " to "ketik ",
            "nyalain " to "nyalakan ",
            "nyalakanlah " to "nyalakan ",
            "matikanlah " to "matikan ",
            "seting " to "setting ",
            "seting " to "settings "
        )

        prefixReplacements.forEach { (wrong, correct) ->
            if (text.startsWith(wrong)) {
                text = correct + text.removePrefix(wrong)
            }
        }

        /*
         * Koreksi kata tunggal yang sering muncul dari Whisper.
         */
        val wordReplacements = mapOf(
            "watshap" to "whatsapp",
            "watsap" to "whatsapp",
            "watsapp" to "whatsapp",
            "wacap" to "whatsapp",
            "wasap" to "whatsapp",
            "youtub" to "youtube",
            "yutub" to "youtube",
            "utube" to "youtube",
            "krom" to "chrome",
            "crom" to "chrome",
            "kamer" to "kamera",
            "cam" to "kamera",
            "galery" to "galeri",
            "galeri" to "galeri",
            "pengaturans" to "pengaturan",
            "pengaturanr" to "pengaturan",
            "settingan" to "settings",
            "seting" to "settings",
            "senter" to "senter",
            "seteng" to "senter",
            "speker" to "speaker",
            "spiker" to "speaker",
            "telpon" to "telepon",
            "tlp" to "telepon",
            "kontakks" to "kontak",
            "catet" to "catatan",
            "catetan" to "catatan",
            "timeran" to "timer"
        )

        text = text
            .split(" ")
            .joinToString(" ") { word ->
                wordReplacements[word] ?: word
            }

        /*
         * Fuzzy correction ringan.
         *
         * Hanya kata dengan panjang minimal 4 karakter dan
         * jarak maksimal 2 karakter yang dikoreksi.
         * Ini mencegah perubahan berlebihan pada kalimat normal.
         */
        val vocabulary = listOf(
            "buka",
            "ketik",
            "nyalakan",
            "matikan",
            "setel",
            "pasang",
            "buat",
            "catatan",
            "senter",
            "whatsapp",
            "youtube",
            "chrome",
            "kamera",
            "galeri",
            "pengaturan",
            "settings",
            "telepon",
            "kontak",
            "timer",
            "speaker",
            "panggilan",
            "angkat",
            "tolak"
        )

        text = text
            .split(" ")
            .joinToString(" ") { word ->
                fuzzyCorrectWord(word, vocabulary)
            }

        return text
    }

    private fun fuzzyCorrectWord(
        word: String,
        vocabulary: List<String>
    ): String {
        if (word.length < 4) {
            return word
        }

        val candidate = vocabulary.minByOrNull { vocabularyWord ->
            editDistance(word, vocabularyWord)
        } ?: return word

        val distance = editDistance(word, candidate)

        return if (distance <= 2) {
            candidate
        } else {
            word
        }
    }

    private fun editDistance(
        first: String,
        second: String
    ): Int {
        val row = IntArray(second.length + 1) { it }

        for (i in first.indices) {
            var diagonal = row[0]
            row[0] = i + 1

            for (j in second.indices) {
                val above = row[j + 1]

                row[j + 1] = if (first[i] == second[j]) {
                    diagonal
                } else {
                    1 + minOf(
                        diagonal,
                        above,
                        row[j]
                    )
                }

                diagonal = above
            }
        }

        return row[second.length]
    }

    private fun extractLastNumber(text: String): Int {
        return Regex("\\d+")
            .findAll(text)
            .lastOrNull()
            ?.value
            ?.toIntOrNull()
            ?: 0
    }
}

sealed interface IntentResult {
    data class OpenApp(
        val name: String
    ) : IntentResult

    data class CallControl(
        val action: String
    ) : IntentResult

    data class CreateNote(
        val body: String
    ) : IntentResult

    data class Timer(
        val minutes: Int
    ) : IntentResult

    data class Flashlight(
        val enabled: Boolean
    ) : IntentResult

    data class Dictation(
        val text: String
    ) : IntentResult
}