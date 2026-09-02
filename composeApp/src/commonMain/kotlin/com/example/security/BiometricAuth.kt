package com.example.security

/**
 * Multiplatform Biometric & Hardware Authentication.
 *
 * Platforms:
 * - Android: BiometricPrompt (Class 3 / Strong Biometrics)
 * - iOS: LocalAuthentication (Face ID / Touch ID)
 * - Desktop: Windows Hello (WinRT/JNA) or Master Passphrase fallback
 * - Web (WasmJS): WebAuthn / Passkey or Session PIN fallback
 */
expect class BiometricAuth() {
    fun canAuthenticate(): Boolean
    suspend fun authenticate(title: String, subtitle: String): Boolean
}
