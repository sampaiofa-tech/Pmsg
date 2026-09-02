package com.example.security

actual class ScreenshotShield {
    actual fun enableProtection() {
        // iOS UITextField isSecureTextEntry subview hack to blank screen in switcher/recording
    }

    actual fun disableProtection() {
        // Remove protection
    }

    actual fun observeCaptureAttempts(onAttemptDetected: () -> Unit) {
        // UIApplicationUserDidTakeScreenshotNotification observer
    }
}
