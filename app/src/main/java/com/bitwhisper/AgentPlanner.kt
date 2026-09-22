package com.bitwhisper

sealed interface PlannedStep {
    data class Tool(val name: String, val argument: String = "", val important: Boolean = false) : PlannedStep
    data class AskConfirmation(val reason: String) : PlannedStep
    data class Reply(val text: String) : PlannedStep
}

class AgentPlanner {
    fun plan(input: String): List<PlannedStep> {
        val parts = input.split(Regex("\\s+(?:lalu|kemudian|terus)\\s+"), ignoreCase = true)
        return parts.flatMap { sentence ->
            val lower = sentence.lowercase().trim()
            when {
                lower.startsWith("buka ") -> listOf(PlannedStep.Tool("open_app", sentence.substringAfter(' ').trim()))
                lower.startsWith("klik ") || lower.startsWith("tekan ") -> listOf(PlannedStep.Tool("click", sentence.substringAfter(' ').trim(), ActionSafety.risk(sentence) == ActionRisk.IMPORTANT))
                lower.startsWith("isi ") -> listOf(PlannedStep.Tool("fill", sentence))
                lower.contains("kirim") || lower.contains("bayar") || lower.contains("hapus") -> listOf(PlannedStep.AskConfirmation(sentence))
                else -> listOf(PlannedStep.Reply(sentence))
            }
        }
    }
}
