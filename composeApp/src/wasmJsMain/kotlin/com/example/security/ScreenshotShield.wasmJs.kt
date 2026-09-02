package com.example.security

actual class ScreenshotShield {
    actual fun enableProtection() {
        // Web: Blur window on tab blur / visibility change
    }

    actual fun disableProtection() {
        // Restore
    }

    actual fun observeCaptureAttempts(onAttemptDetected: () -> Unit) {
        // Keydown PrintScreen capture notification
    }
}
