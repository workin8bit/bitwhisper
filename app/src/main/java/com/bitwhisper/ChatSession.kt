package com.bitwhisper

/** Keeps a bounded in-memory context while ConversationStore persists the transcript. */
class ChatSession(private val mode: ChatMode = ChatMode.GENERAL) {
    private val turns = ArrayDeque<Pair<String, String>>()
    fun add(user: String, assistant: String) {
        if (turns.size >= MAX_TURNS) turns.removeFirst()
        turns.addLast(user to assistant)
    }
    fun prompt(user: String): String = ChatPromptBuilder.build(mode, user, turns.toList())
    fun history(): List<Pair<String, String>> = turns.toList()
    fun clear() = turns.clear()
    companion object { private const val MAX_TURNS = 10 }
}
