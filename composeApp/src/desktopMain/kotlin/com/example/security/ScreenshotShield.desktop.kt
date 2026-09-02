package com.example.security

actual class ScreenshotShield {
    actual fun enableProtection() {
        // Desktop window display affinity / privacy mode
    }

    actual fun disableProtection() {
        // Desktop window privacy mode disabled
    }

    actual fun observeCaptureAttempts(onAttemptDetected: () -> Unit) {
        // Desktop screenshot capture hooks
    }
}
