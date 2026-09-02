package com.example.security

import android.app.Activity
import android.view.WindowManager

actual class ScreenshotShield {
    private var activityProvider: (() -> Activity)? = null

    fun bindActivity(activity: Activity) {
        activityProvider = { activity }
    }

    actual fun enableProtection() {
        activityProvider?.invoke()?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    actual fun disableProtection() {
        activityProvider?.invoke()?.window?.clearFlags(
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    actual fun observeCaptureAttempts(onAttemptDetected: () -> Unit) {
        // Connected via Activity ScreenshotDetector
    }
}
