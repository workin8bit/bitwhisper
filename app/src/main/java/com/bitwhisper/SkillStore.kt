package com.bitwhisper

import android.content.Context

data class LocalSkill(val name: String, val trigger: String, val action: String, val enabled: Boolean = true)

class SkillStore(context: Context) {
    private val prefs = context.getSharedPreferences("skills", Context.MODE_PRIVATE)
    fun save(skill: LocalSkill) { prefs.edit().putString(skill.name, "${skill.trigger}|${skill.action}|${skill.enabled}").apply() }
    fun find(input: String): LocalSkill? = prefs.all.entries.asSequence().mapNotNull { (name, raw) ->
        val parts = raw.toString().split('|', limit = 3)
        if (parts.size == 3 && input.contains(parts[0], true) && parts[2] == "true") LocalSkill(name, parts[0], parts[1]) else null
    }.firstOrNull()
    fun names() = prefs.all.keys.toList()
    fun remove(name: String) = prefs.edit().remove(name).apply()
}
