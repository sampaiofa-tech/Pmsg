package com.example.security

actual class BiometricAuth {
    actual fun canAuthenticate(): Boolean = false // Web fallback to session password/PIN

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // Fallback to in-session PIN modal
        return true
    }
}
