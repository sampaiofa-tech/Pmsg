package com.example.security

/**
 * Multiplatform Anti-Snooping & Anti-Screenshot Shield.
 *
 * Implementations:
 * - Android: WindowManager.LayoutParams.FLAG_SECURE + ContentObserver for screenshot capture events.
 * - iOS: Secure overlay UITextField on UIWindow to blank app preview in multitasking and captures.
 * - Desktop: OS-level display affinity (SetWindowDisplayAffinity on Windows) or no-op warning log.
 * - Web (WasmJS): Canvas overlay / Page Visibility API blur with security notice.
 */
expect class ScreenshotShield() {
    fun enableProtection()
    fun disableProtection()
    fun observeCaptureAttempts(onAttemptDetected: () -> Unit)
}
