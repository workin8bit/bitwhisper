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
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("last_notification", listOf(title, text).filter { it.isNotBlank() }.joinToString(": "))
                .apply()
        }
    }

    companion object {
        const val PREFS = "notification_memory"
    }
}
