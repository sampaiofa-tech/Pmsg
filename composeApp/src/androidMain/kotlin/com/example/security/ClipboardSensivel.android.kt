package com.example.security

import android.content.Context
import com.example.util.security.ClipboardSanitizer

actual class ClipboardSensivel {
    private var contextProvider: (() -> Context)? = null

    fun setContext(context: Context) {
        contextProvider = { context }
    }

    actual fun copySensitive(text: String, label: String) {
        val ctx = contextProvider?.invoke() ?: return
        ClipboardSanitizer.copySensitiveText(ctx, label, text)
    }

    actual fun clear() {
        val ctx = contextProvider?.invoke() ?: return
        ClipboardSanitizer.sanitizeNow(ctx)
    }
}
