package com.example.security.identity

/**
 * Multiplatform AES-256-GCM (NIST SP 800-38D) primitive.
 * Authenticated Encryption with Associated Data (AEAD) using 128-bit authentication tag.
 */
expect object AesGcm {
    fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
}
