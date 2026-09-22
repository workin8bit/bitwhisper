package com.bitwhisper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConversationEntry(val user: String, val assistant: String, val time: Long)

class ConversationStore(context: Context) {
    private val prefs = context.getSharedPreferences("conversation", Context.MODE_PRIVATE)
    fun add(user: String, assistant: String) {
        val items = JSONArray(prefs.getString("items", "[]"))
        items.put(JSONObject().apply { put("user", user); put("assistant", assistant); put("time", System.currentTimeMillis()) })
        while (items.length() > 100) items.remove(0)
        prefs.edit().putString("items", items.toString()).apply()
    }
    fun all(): List<ConversationEntry> {
        val items = JSONArray(prefs.getString("items", "[]"))
        return (0 until items.length()).map { val o = items.getJSONObject(it); ConversationEntry(o.getString("user"), o.getString("assistant"), o.getLong("time")) }
    }
    fun clear() = prefs.edit().remove("items").apply()
}
