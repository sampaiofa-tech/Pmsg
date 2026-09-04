package com.example.security.identity

/**
 * Pure Kotlin implementation of HMAC-SHA256 (RFC 2104) and HKDF-SHA256 (RFC 5869).
 *
 * Fully deterministic across all CMP targets (Android, Desktop JVM, iOS, and WasmJS)
 * without external dependencies, built upon [Sha256Digest].
 */
object HmacSha256 {

    private const val BLOCK_SIZE = 64 // 512 bits for SHA-256
    private const val IPAD_BYTE: Byte = 0x36
    private const val OPAD_BYTE: Byte = 0x5c

    fun compute(key: ByteArray, message: ByteArray): ByteArray {
        val normalizedKey = if (key.size > BLOCK_SIZE) {
            Sha256Digest.digest(key)
        } else {
            key
        }

        val keyBlock = ByteArray(BLOCK_SIZE)
        normalizedKey.copyInto(keyBlock, 0, 0, normalizedKey.size)

        val iPad = ByteArray(BLOCK_SIZE)
        val oPad = ByteArray(BLOCK_SIZE)
        for (i in 0 until BLOCK_SIZE) {
            iPad[i] = (keyBlock[i].toInt() xor IPAD_BYTE.toInt()).toByte()
            oPad[i] = (keyBlock[i].toInt() xor OPAD_BYTE.toInt()).toByte()
        }

        val innerHash = Sha256Digest.digest(iPad + message)
        return Sha256Digest.digest(oPad + innerHash)
    }
}

object HkdfSha256 {

    private const val HASH_LEN = 32 // 256 bits for SHA-256

    /**
     * HKDF-Extract(salt, IKM) -> PRK
     */
    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val effectiveSalt = if (salt == null || salt.isEmpty()) {
            ByteArray(HASH_LEN) // 32 zeros
        } else {
            salt
        }
        return HmacSha256.compute(effectiveSalt, ikm)
    }

    /**
     * HKDF-Expand(PRK, info, L) -> OKM
     */
    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length > 0) { "Length must be positive" }
        require(length <= 255 * HASH_LEN) { "Cannot expand to more than 255 * 32 bytes" }

        val okm = ByteArray(length)
        var previousT = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            val input = previousT + info + byteArrayOf(counter.toByte())
            previousT = HmacSha256.compute(prk, input)

            val bytesToCopy = minOf(HASH_LEN, length - offset)
            previousT.copyInto(okm, offset, 0, bytesToCopy)
            offset += bytesToCopy
            counter++
        }

        return okm
    }

    /**
     * HKDF(ikm, salt, info, length) = Expand(Extract(salt, ikm), info, length)
     */
    fun deriveKey(ikm: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = 32): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}
