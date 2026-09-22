package com.bitwhisper

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class BitWhisperAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var longPressStarted = false
    private val startTask = Runnable { longPressStarted = true; startRecording() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
        serviceInfo = serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    longPressStarted = false
                    handler.postDelayed(startTask, LONG_PRESS_MS)
                }
                return true
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(startTask)
                if (longPressStarted) stopRecording()
                longPressStarted = false
                return true
            }
        }
        return false
    }

    private fun startRecording() = sendBroadcast(Intent(ACTION_RECORD_START).setPackage(packageName))
    private fun stopRecording() = sendBroadcast(Intent(ACTION_RECORD_STOP).setPackage(packageName))
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { if (active === this) active = null; super.onDestroy() }

    fun insertText(text: String): Boolean {
        val node = rootInActiveWindow?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply { putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    companion object {
        const val ACTION_RECORD_START = "com.bitwhisper.RECORD_START"
        const val ACTION_RECORD_STOP = "com.bitwhisper.RECORD_STOP"
        private const val LONG_PRESS_MS = 450L
        @Volatile var active: BitWhisperAccessibilityService? = null
    }
}
