package com.bitwhisper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Stores only the latest notification text locally so it can be read on demand. */
class BitWhisperNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isNotBlank() || text.isNotBlank()) {
            val value = listOf(title, text).filter { it.isNotBlank() }.joinToString(": ")
            val isCall = sbn.notification.category == android.app.Notification.CATEGORY_CALL ||
                value.contains("panggilan", true) || value.contains("incoming call", true)
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("last_notification", value)
                .putBoolean("incoming_call", isCall)
                .putString("caller_label", value)
                .apply()
            if (isCall) {
                val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                val previous = prefs.getString("announced_call", null)
                if (previous != value) {
                    prefs.edit().putString("announced_call", value).apply()
                    sendBroadcast(android.content.Intent(ACTION_INCOMING_CALL).setPackage(packageName).putExtra("label", value))
                }
            }
        }
    }

    companion object {
        const val PREFS = "notification_memory"
        const val ACTION_INCOMING_CALL = "com.bitwhisper.INCOMING_CALL"
    }
}
