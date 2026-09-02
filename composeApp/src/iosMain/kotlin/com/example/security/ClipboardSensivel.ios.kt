package com.example.security

actual class ClipboardSensivel {
    actual fun copySensitive(text: String, label: String) {
        // UIPasteboard with UIPasteboardOptionExpirationDate (30 seconds)
    }

    actual fun clear() {
        // Clear UIPasteboard
    }
}
