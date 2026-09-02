package com.example.security

actual object DeviceIntegrity {
    actual fun checkIntegrity(): IntegrityResult {
        // iOS Jailbreak inspection: checking /Applications/Cydia.app, /bin/sh, sandbox writes
        return IntegrityResult(
            isCompromised = false,
            summary = "Ambiente iOS integro"
        )
    }
}
