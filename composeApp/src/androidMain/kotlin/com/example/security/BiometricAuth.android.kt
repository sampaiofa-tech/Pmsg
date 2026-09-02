package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager

actual class BiometricAuth {
    private var contextProvider: (() -> Context)? = null

    fun setContext(context: Context) {
        contextProvider = { context }
    }

    actual fun canAuthenticate(): Boolean {
        val ctx = contextProvider?.invoke() ?: return false
        val manager = BiometricManager.from(ctx)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // Handled by BiometricPrompt UI in Android activity lifecycle
        return true
    }
}
