package com.example.security

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
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

    private val dpapiStorageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "master.dpapi")
    }

    private fun getOrCreateKey(): SecretKey {
        return masterKey ?: synchronized(this) {
            masterKey ?: loadOrGenerateKey().also { masterKey = it }
        }
    }

    private fun loadOrGenerateKey(): SecretKey {
        if (Platform.isWindows() && dpapiStorageFile.exists()) {
            try {
                val dpapiEncryptedBytes = dpapiStorageFile.readBytes()
                val rawKeyBytes = Crypt32Util.cryptUnprotectData(dpapiEncryptedBytes)
                return SecretKeySpec(rawKeyBytes, ALGORITHM)
            } catch (_: Exception) {
                // Se a chave corrompeu ou mudou de usuário, regenera protegida
            }
        }

        // Gera nova chave de 256 bits
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(256)
        val newKey = keyGen.generateKey()

        // Persiste em repouso protegida via Windows DPAPI (CNG/CryptoAPI)
        if (Platform.isWindows()) {
            try {
                val protectedBytes = Crypt32Util.cryptProtectData(newKey.encoded)
                dpapiStorageFile.writeBytes(protectedBytes)
            } catch (_: Exception) {
                // Fallback em memória caso DPAPI nativo falhe
            }
        }
        return newKey
    }

    actual fun isHardwareBacked(): Boolean = false // JVM Software Provider com DPAPI em repouso

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
            val newKey = keyGen.generateKey()
            masterKey = newKey

            if (Platform.isWindows()) {
                try {
                    val protectedBytes = Crypt32Util.cryptProtectData(newKey.encoded)
                    dpapiStorageFile.writeBytes(protectedBytes)
                } catch (_: Exception) {
                    if (dpapiStorageFile.exists()) dpapiStorageFile.delete()
                }
            }
        }
    }

    actual fun generateSecureNoise(length: Int): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
