package com.example.security

actual object AppCheckVerifier {
    actual suspend fun getAttestationToken(): String? {
        // iOS DeviceCheck / App Attest token
        return "APP_ATTEST_DEVICE_CHECK_TOKEN"
    }
}
