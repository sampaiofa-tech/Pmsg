package com.example.security.identity

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object AesGcm {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128

    actual fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        require(iv.size == 12) { "GCM requires a 12-byte IV" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(plaintext)
    }

    actual fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        require(iv.size == 12) { "GCM requires a 12-byte IV" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }
}
