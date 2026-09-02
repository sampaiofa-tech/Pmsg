package com.example.security

actual object AppCheckVerifier {
    actual suspend fun getAttestationToken(): String? {
        // Web reCAPTCHA Enterprise client token
        return "RECAPTCHA_ENTERPRISE_WEB_TOKEN"
    }
}
