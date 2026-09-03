package com.example.security

actual object AppCheckVerifier {
    actual suspend fun getAttestationToken(): String? {
        // Desktop JVM: Obtém ID Token autêntico do Firebase Auth via REST API (Identity Toolkit)
        return DesktopAuthManager.getIdToken()
    }
}

