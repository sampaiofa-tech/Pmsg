package com.example.security

/**
 * Multiplatform Panic Trigger / Emergency Gesture.
 *
 * Implementations:
 * - Android: Accelerometer shake detection (SensorManager) with peak threshold.
 * - iOS: CMMotionManager shake gesture.
 * - Desktop: Global keyboard hotkey (Ctrl + Shift + P) or escape key sequence.
 * - Web (WasmJS): Keyboard shortcut or on-screen emergency trigger.
 */
expect class PanicGesture() {
    fun startListening(onPanicTriggered: () -> Unit)
    fun stopListening()
}
