package dev.xitee.sleeptimer.core.service.screen

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service whose only purpose is performing
 * [AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN] when the timer expires. It
 * requests no event types and no window-content access (see
 * `res/xml/accessibility_lock_service.xml` in the app module). The system
 * instantiates and binds it while the user has it enabled in the accessibility
 * settings; [AccessibilityLockHelper] reaches the live instance via [instance].
 *
 * Must stay Hilt-free: the system creates it through the no-arg constructor.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: LockAccessibilityService? = null
            private set
    }
}
