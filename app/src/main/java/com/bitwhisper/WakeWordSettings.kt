package com.bitwhisper

import android.content.Context

class WakeWordSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val phrase: String get() = prefs.getString("wake_word", "kyu kyu") ?: "kyu kyu"
    val enabled: Boolean get() = prefs.getBoolean("wake_word_enabled", true)
    fun setPhrase(value: String) { prefs.edit().putString("wake_word", value.trim()).apply() }
    fun setEnabled(value: Boolean) { prefs.edit().putBoolean("wake_word_enabled", value).apply() }
}
