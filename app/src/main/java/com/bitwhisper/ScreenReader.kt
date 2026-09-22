package com.bitwhisper

import android.view.accessibility.AccessibilityNodeInfo

class ScreenReader(private val service: BitWhisperAccessibilityService) {
    fun dump(): String = buildString { service.rootInActiveWindow?.let { appendNode(it, 0) } }
    private fun appendNode(node: AccessibilityNodeInfo, depth: Int) {
        val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" | ")
        if (label.isNotBlank()) append(" ".repeat(depth * 2)).append(label).append("\n")
        for (index in 0 until node.childCount) node.getChild(index)?.let { appendNode(it, depth + 1) }
    }
}
