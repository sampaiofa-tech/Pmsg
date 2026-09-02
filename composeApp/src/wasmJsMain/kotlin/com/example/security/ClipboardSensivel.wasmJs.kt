package com.example.security

actual class ClipboardSensivel {
    actual fun copySensitive(text: String, label: String) {
        // Web navigator.clipboard.writeText
    }

    actual fun clear() {
        // Web navigator.clipboard.writeText("")
    }
}
