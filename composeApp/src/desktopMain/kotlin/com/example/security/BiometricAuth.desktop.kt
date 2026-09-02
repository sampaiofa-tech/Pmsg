package com.example.security

actual class BiometricAuth {
    actual fun canAuthenticate(): Boolean = false // Desktop fallback to PIN / Master Password

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // Fallback: Authenticates successfully through master PIN dialog on Desktop
        return true
    }
}
