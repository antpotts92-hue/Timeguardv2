package com.timeguard.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ScrollService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        ScrollStore.record(this, pkg, System.currentTimeMillis())
    }
    override fun onInterrupt() = Unit
}
