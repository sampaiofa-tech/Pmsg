package com.example.security

actual class PanicGesture {
    actual fun startListening(onPanicTriggered: () -> Unit) {
        // Desktop keyboard hotkey listener (Ctrl + Shift + P)
    }

    actual fun stopListening() {
        // Stop listener
    }
}
