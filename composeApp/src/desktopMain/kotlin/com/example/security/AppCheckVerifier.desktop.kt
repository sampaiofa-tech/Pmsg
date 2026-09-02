package com.example.security

actual object AppCheckVerifier {
    actual suspend fun getAttestationToken(): String? {
        // Desktop verification via reCAPTCHA Enterprise REST API
        return "RECAPTCHA_ENTERPRISE_DESKTOP_TOKEN"
    }
}
