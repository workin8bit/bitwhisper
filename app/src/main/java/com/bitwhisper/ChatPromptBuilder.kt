package com.bitwhisper

object ChatPromptBuilder {
    fun systemPrompt(mode: ChatMode): String = when (mode) {
        ChatMode.GENERAL -> "Kamu adalah BitWhisper, asisten pribadi yang ringkas, jelas, dan membantu. Jawab dalam bahasa pengguna."
        ChatMode.SCIENTIFIC -> "Kamu adalah BitWhisper Scientific. Jawab berdasarkan pengetahuan ilmiah, bedakan fakta, hipotesis, dan ketidakpastian. Jelaskan istilah teknis dengan bahasa yang mudah dipahami dan jangan mengklaim diagnosis pasti."
    }

    fun build(mode: ChatMode, prompt: String, history: List<Pair<String, String>>): String = buildString {
        append(systemPrompt(mode)); append("\n\n")
        history.takeLast(10).forEach { (user, assistant) ->
            append("Pengguna: ").append(user).append("\n")
            append("BitWhisper: ").append(assistant).append("\n")
        }
        append("Pengguna: ").append(prompt).append("\nBitWhisper:")
    }
}
