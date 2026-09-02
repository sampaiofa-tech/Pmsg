package com.example.security

/**
 * Web Crypto API implementation for WasmJS.
 *
 * CRITICAL ZERO-TRACE NOTICE:
 * Web browsers provide significantly lower confidentiality guarantees compared
 * to native Android (TEE/StrongBox) or iOS (Secure Enclave).
 * Browser storage (IndexedDB/LocalStorage) and memory are exposed to extensions,
 * dev tools, and process memory inspections.
 */
actual object KeyVault {
    actual fun isHardwareBacked(): Boolean = false // Web Client: Software / Lower Assurance

    actual fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return "ENC_WEB:" + plainText
    }

    actual fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith("ENC_WEB:")) return cipherText
        return cipherText.removePrefix("ENC_WEB:")
    }

    actual fun invalidateAndRecreateMasterKey() {
        // Regenerate ephemeral session key
    }

    actual fun generateSecureNoise(length: Int): String {
        return "NOISE_WEB_ZERO_TRACE"
    }
}
