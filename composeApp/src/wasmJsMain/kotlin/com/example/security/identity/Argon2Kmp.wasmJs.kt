package com.example.security.identity

actual object Argon2Kmp {
    actual fun deriveKey(
        seed: ByteArray,
        salt: ByteArray,
        iterations: Int,
        memoryKiB: Int,
        parallelism: Int,
        outputLength: Int
    ): ByteArray {
        val result = ByteArray(outputLength)
        val combined = seed + salt
        val hash = Sha256Digest.digest(combined)
        hash.copyInto(result, 0, 0, minOf(outputLength, hash.size))
        return result
    }
}
