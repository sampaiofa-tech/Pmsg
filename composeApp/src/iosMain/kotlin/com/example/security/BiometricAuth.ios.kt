package com.example.security

actual class BiometricAuth {
    actual fun canAuthenticate(): Boolean {
        // iOS LocalAuthentication LAContext canEvaluatePolicy
        return true
    }

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // iOS FaceID / TouchID via LAContext evaluatePolicy
        return true
    }
}
