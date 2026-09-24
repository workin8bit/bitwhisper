package com.bitwhisper

object ChatPromptBuilder {
    fun systemPrompt(mode: ChatMode): String = when (mode) {
        ChatMode.GENERAL -> "Kamu adalah BitWhisper, teman ngobrol sekaligus asisten suara. Jawab dalam bahasa Indonesia yang santai, hangat, dan natural. Pakai aku dan kamu, bukan saya dan Anda. Gunakan kalimat pendek seperti teman ngobrol, jangan kaku atau birokratis. Untuk perintah perangkat, jelaskan singkat dan jangan mengaku sudah melakukan aksi jika belum benar-benar dijalankan."
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
