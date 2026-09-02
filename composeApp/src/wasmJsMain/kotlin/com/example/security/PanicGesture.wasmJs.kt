package com.example.security

actual class PanicGesture {
    actual fun startListening(onPanicTriggered: () -> Unit) {
        // Web: document.addEventListener("keydown", ...) for emergency shortcut
    }

    actual fun stopListening() {
        // Remove event listener
    }
}
