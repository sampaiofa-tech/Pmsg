package com.example.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object KeyVault {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val PREFIX = "ENC_DESKTOP:"

    @Volatile
    private var masterKey: SecretKey? = null

    private fun getOrCreateKey(): SecretKey {
        return masterKey ?: synchronized(this) {
            masterKey ?: run {
                val keyGen = KeyGenerator.getInstance(ALGORITHM)
                keyGen.init(256)
                keyGen.generateKey().also { masterKey = it }
            }
        }
    }

    actual fun isHardwareBacked(): Boolean = false // JVM Software Provider

    actual fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return PREFIX + Base64.getEncoder().encodeToString(combined)
    }

    actual fun decrypt(cipherText: String): String {
        if (!cipherText.startsWith(PREFIX)) return cipherText
        return try {
            val combined = Base64.getDecoder().decode(cipherText.removePrefix(PREFIX))
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plain = cipher.doFinal(cipherBytes)
            String(plain, Charsets.UTF_8)
        } catch (_: Exception) {
            "🔒 [Mensagem Criptografada]"
        }
    }

    actual fun invalidateAndRecreateMasterKey() {
        synchronized(this) {
            val keyGen = KeyGenerator.getInstance(ALGORITHM)
            keyGen.init(256)
            masterKey = keyGen.generateKey()
        }
    }

    actual fun generateSecureNoise(length: Int): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
