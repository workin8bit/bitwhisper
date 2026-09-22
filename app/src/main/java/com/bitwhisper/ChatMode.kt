package com.bitwhisper

enum class ChatMode(val key: String, val label: String) {
    GENERAL("general", "Umum"),
    SCIENTIFIC("scientific", "Scientific")
}

class ChatSettings(private val context: android.content.Context) {
    private val prefs = context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    fun mode(): ChatMode = ChatMode.values().firstOrNull { it.key == prefs.getString("chat_mode", ChatMode.GENERAL.key) } ?: ChatMode.GENERAL
    fun setMode(mode: ChatMode) { prefs.edit().putString("chat_mode", mode.key).apply() }
}
