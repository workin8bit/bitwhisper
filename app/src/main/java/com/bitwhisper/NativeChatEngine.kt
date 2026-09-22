package com.bitwhisper

import android.content.Context

class LocalChatEngine(context: Context) : OfflineChatEngine {
    private val models = ModelManager(context)
    override suspend fun respond(prompt: String, history: List<Pair<String, String>>, mode: ChatMode): String {
        val fullPrompt = ChatPromptBuilder.build(mode, prompt, history)
        if (!models.isChatReady() || !NativeInference.isAvailable()) {
            return "Model chatbot lokal belum siap. Pasang model GGUF dan runtime native terlebih dahulu."
        }
        return NativeInference.chat(models.chatModel, fullPrompt)
    }
}
