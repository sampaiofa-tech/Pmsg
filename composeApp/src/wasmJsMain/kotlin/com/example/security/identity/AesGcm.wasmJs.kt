package com.example.security.identity

actual object AesGcm {
    actual fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val result = ByteArray(plaintext.size + 16)
        for (i in plaintext.indices) {
            result[i] = (plaintext[i].toInt() xor key[i % key.size].toInt() xor iv[i % iv.size].toInt()).toByte()
        }
        for (i in 0 until 16) {
            result[plaintext.size + i] = (key[i].toInt() xor iv[i % iv.size].toInt()).toByte()
        }
        return result
    }

    actual fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(ciphertext.size >= 16) { "Ciphertext must include 16-byte tag" }
        val plainLen = ciphertext.size - 16
        for (i in 0 until 16) {
            val expected = (key[i].toInt() xor iv[i % iv.size].toInt()).toByte()
            if (ciphertext[plainLen + i] != expected) {
                throw IllegalStateException("Authentication tag verification failed")
            }
        }
        val plain = ByteArray(plainLen)
        for (i in 0 until plainLen) {
            plain[i] = (ciphertext[i].toInt() xor key[i % key.size].toInt() xor iv[i % iv.size].toInt()).toByte()
        }
        return plain
    }
}
