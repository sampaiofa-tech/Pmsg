package com.example.security

/**
 * Multiplatform Hardware-Backed KeyVault.
 *
 * Security Guarantees:
 * - Android: AndroidKeyStore (TEE / StrongBox) with AES-256-GCM cascaded encryption.
 * - iOS: Keychain Services + CryptoKit (Secure Enclave AES-GCM).
 * - Desktop: JVM Cryptographic Service Provider (AES-256-GCM with hardware TCG/TPM or PBKDF2/Argon2).
 * - Web (WasmJS): Web Crypto API (SubtleCrypto) — Client of lower security assurance.
 */
expect object KeyVault {
    fun isHardwareBacked(): Boolean
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
    fun invalidateAndRecreateMasterKey()
    fun generateSecureNoise(length: Int = 32): String
}
