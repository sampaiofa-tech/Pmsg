package com.example.security

actual class PanicGesture {
    actual fun startListening(onPanicTriggered: () -> Unit) {
        // CMMotionManager shake acceleration tracking / UIEventSubtypeMotionShake
    }

    actual fun stopListening() {
        // Stop motion updates
    }
}
