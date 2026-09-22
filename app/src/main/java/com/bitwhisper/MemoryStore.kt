package com.bitwhisper

import android.content.Context

class MemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("memory", Context.MODE_PRIVATE)
    fun remember(key: String, value: String) { prefs.edit().putString(key.trim().lowercase(), value.trim()).apply() }
    fun recall(key: String): String? = prefs.getString(key.trim().lowercase(), null)
    fun all(): Map<String, *> = prefs.all
    fun clear() = prefs.edit().clear().apply()
}
