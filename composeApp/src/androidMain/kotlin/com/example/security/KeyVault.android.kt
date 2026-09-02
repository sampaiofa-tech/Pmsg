package com.example.security

import com.example.util.security.CryptoManager

actual object KeyVault {
    actual fun isHardwareBacked(): Boolean = CryptoManager.isHardwareBacked()
    actual fun encrypt(plainText: String): String = CryptoManager.encrypt(plainText)
    actual fun decrypt(cipherText: String): String = CryptoManager.decrypt(cipherText)
    actual fun invalidateAndRecreateMasterKey() = CryptoManager.invalidateAndRecreateMasterKey()
    actual fun generateSecureNoise(length: Int): String = CryptoManager.generateSecureNoise(length)
}
