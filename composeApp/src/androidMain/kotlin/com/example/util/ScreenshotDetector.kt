package com.example.util

import android.app.Activity
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import java.lang.ref.WeakReference

/**
 * Robust utility to detect screenshots on Android devices.
 * Uses Android 14+ native ScreenCaptureCallback with ContentObserver fallback.
 * Uses WeakReference to prevent Activity memory leaks.
 */
class ScreenshotDetector(
    activity: Activity,
    private val onScreenshotDetected: () -> Unit
) {
    private val activityRef = WeakReference(activity)
    private var isListening = false
    private var screenCaptureCallback: Any? = null
    private var contentObserver: ContentObserver? = null

    fun startListening() {
        if (isListening) return
        val activity = activityRef.get() ?: return
        isListening = true

        // 1. Android 14+ (API 34+) Official ScreenCaptureCallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val callback = Activity.ScreenCaptureCallback {
                    Log.d("ScreenshotDetector", "Android 14+ Screen capture callback triggered")
                    onScreenshotDetected()
                }
                screenCaptureCallback = callback
                activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
            } catch (e: Throwable) {
                Log.w("ScreenshotDetector", "ScreenCaptureCallback unavailable or permission missing: ${e.message}")
            }
        }

        // 2. ContentObserver fallback on MediaStore
        try {
            val handler = Handler(Looper.getMainLooper())
            contentObserver = object : ContentObserver(handler) {
                private var lastDetectedTime = 0L

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    val now = System.currentTimeMillis()
                    // Debounce rapid events within 1.5 seconds
                    if (now - lastDetectedTime > 1500) {
                        lastDetectedTime = now
                        Log.d("ScreenshotDetector", "MediaStore content change detected: $uri")
                        onScreenshotDetected()
                    }
                }
            }

            contentObserver?.let { observer ->
                activity.contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
            }
        } catch (e: Exception) {
            Log.e("ScreenshotDetector", "Failed to register ContentObserver for screenshots", e)
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        val activity = activityRef.get()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && activity != null) {
            screenCaptureCallback?.let { callback ->
                try {
                    activity.unregisterScreenCaptureCallback(callback as Activity.ScreenCaptureCallback)
                } catch (e: Throwable) {
                    Log.w("ScreenshotDetector", "Failed to unregister ScreenCaptureCallback: ${e.message}")
                }
            }
            screenCaptureCallback = null
        }

        if (activity != null) {
            contentObserver?.let { observer ->
                try {
                    activity.contentResolver.unregisterContentObserver(observer)
                } catch (e: Exception) {
                    Log.e("ScreenshotDetector", "Failed to unregister ContentObserver", e)
                }
            }
        }
        contentObserver = null
    }
}
