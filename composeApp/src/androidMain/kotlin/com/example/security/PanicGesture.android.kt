package com.example.security

import android.content.Context
import com.example.util.ShakeDetector

actual class PanicGesture {
    private var contextProvider: (() -> Context)? = null
    private var shakeDetector: ShakeDetector? = null

    fun initialize(context: Context) {
        contextProvider = { context }
    }

    actual fun startListening(onPanicTriggered: () -> Unit) {
        val ctx = contextProvider?.invoke() ?: return
        shakeDetector = ShakeDetector(ctx) {
            onPanicTriggered()
        }
        shakeDetector?.startListening()
    }

    actual fun stopListening() {
        shakeDetector?.stopListening()
        shakeDetector = null
    }
}
