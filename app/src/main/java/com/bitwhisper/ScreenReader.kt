package com.bitwhisper

import android.view.accessibility.AccessibilityNodeInfo

class ScreenReader(private val service: BitWhisperAccessibilityService) {
    fun dump(): String = buildString { service.rootInActiveWindow?.let { appendNode(this, it, 0) } }

    private fun appendNode(output: StringBuilder, node: AccessibilityNodeInfo, depth: Int) {
        val label = listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).joinToString(" | ")
        if (label.isNotBlank()) output.append(" ".repeat(depth * 2)).append(label).append('\n')
        for (index in 0 until node.childCount) node.getChild(index)?.let { appendNode(output, it, depth + 1) }
    }
}
