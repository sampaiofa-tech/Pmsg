package com.example.util.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log

/**
 * Zero-Trace Clipboard Sanitizer.
 * Ensures sensitive copied texts (e.g., decrypted messages, channel codes) do not persist
 * indefinitely in the system clipboard or keyboard suggestion buffers.
 * Automatically clears the clipboard after a configurable timeout (default 30s) or immediately upon request.
 */
object ClipboardSanitizer {

    private const val TAG = "ClipboardSanitizer"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingClearRunnable: Runnable? = null
    private var lastCopiedTextHash: Int? = null

    /**
     * Copies sensitive text to the clipboard with:
     * 1. EXTRA_IS_SENSITIVE flag (Android 13+ to prevent keyboard learning & clip overlays).
     * 2. Automatic clipboard scrubbing after [autoClearSeconds] (default: 30s).
     */
    fun copySensitiveText(
        context: Context,
        label: String = "Pmsg Sensitive",
        text: String,
        autoClearSeconds: Int = 30,
        onCleared: (() -> Unit)? = null
    ) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        try {
            val clipData = ClipData.newPlainText(label, text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                clipData.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            clipboardManager.setPrimaryClip(clipData)
            lastCopiedTextHash = text.hashCode()

            // Cancel any previously scheduled clipboard wipe
            pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }

            // Schedule automatic zero-trace wipe
            if (autoClearSeconds > 0) {
                val clearRunnable = Runnable {
                    try {
                        sanitizeIfMatches(context, lastCopiedTextHash)
                        onCleared?.invoke()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error during automated clipboard purge: ${e.message}")
                    }
                }
                pendingClearRunnable = clearRunnable
                mainHandler.postDelayed(clearRunnable, autoClearSeconds * 1000L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy sensitive text securely", e)
        }
    }

    /**
     * Immediately sanitizes / zeroes out the clipboard if it currently holds Pmsg sensitive data.
     */
    fun sanitizeIfMatches(context: Context, expectedHash: Int?) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        try {
            val currentClip = clipboardManager.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0).text?.toString()
                if (expectedHash == null || currentText?.hashCode() == expectedHash) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboardManager.clearPrimaryClip()
                    } else {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                    Log.d(TAG, "Zero-Trace: Clipboard sanitized successfully.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard sanitization skipped: ${e.message}")
        }
    }

    /**
     * Unconditionally wipes the primary clipboard (used on Panic Wipe and app lock).
     */
    fun sanitizeNow(context: Context) {
        pendingClearRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingClearRunnable = null
        lastCopiedTextHash = null
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboardManager.clearPrimaryClip()
            } else {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (_: Exception) {}
    }
}
