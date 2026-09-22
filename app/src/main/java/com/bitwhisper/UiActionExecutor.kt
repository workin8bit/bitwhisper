package com.bitwhisper

import android.view.accessibility.AccessibilityNodeInfo

class UiActionExecutor(private val service: BitWhisperAccessibilityService) {
    fun find(query: String): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return root.findAccessibilityNodeInfosByText(query).firstOrNull()
            ?: findByDescription(root, query)
    }

    fun click(query: String): Boolean {
        if (ActionSafety.risk(query) == ActionRisk.IMPORTANT) return false
        return clickConfirmed(query)
    }

    fun clickConfirmed(query: String): Boolean = find(query)?.let {
        clickableAncestor(it)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    } ?: false

    fun setText(query: String, value: String): Boolean {
        val node = find(query) ?: service.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = android.os.Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scrollForward(): Boolean = service.rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) == true
    fun scrollBackward(): Boolean = service.rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) == true

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) { if (current.isClickable) return current; current = current.parent }
        return null
    }

    private fun findByDescription(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.contains(query, true) == true) return node
        for (index in 0 until node.childCount) node.getChild(index)?.let { findByDescription(it, query)?.let { found -> return found } }
        return null
    }
}
