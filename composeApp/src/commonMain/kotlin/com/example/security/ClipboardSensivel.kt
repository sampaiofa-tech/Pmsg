package com.example.security

/**
 * Multiplatform Sensitive Clipboard Sanitizer.
 *
 * Implements ephemeral clipboard lifecycle:
 * - Android: ClipData with EXTRA_IS_SENSITIVE flag (Android 13+) + coroutine auto-wipe in 30s.
 * - iOS: UIPasteboard with expiration date of 30 seconds.
 * - Desktop: System Clipboard with 30-second automated overwrite.
 * - Web (WasmJS): Navigator.clipboard write + 30-second wipe timer.
 */
expect class ClipboardSensivel() {
    fun copySensitive(text: String, label: String = "Raix Segredo")
    fun clear()
}
