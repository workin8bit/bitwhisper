package com.bitwhisper

/**
 * Boundary for the offline chatbot. A llama.cpp/ GGUF implementation can be
 * plugged in without changing the voice, command, or accessibility layers.
 */
interface OfflineChatEngine {
    fun respond(prompt: String, history: List<Pair<String, String>> = emptyList(), mode: ChatMode = ChatMode.GENERAL): String
}

class UnavailableChatEngine : OfflineChatEngine {
    override suspend fun respond(prompt: String, history: List<Pair<String, String>>, mode: ChatMode): String =
        "Mode chatbot lokal belum diaktifkan. Silakan pasang model GGUF BitWhisper."
}
