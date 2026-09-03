package com.example.security.identity

/**
 * Multiplatform Argon2id Key Derivation Function.
 * Used for deriving 256-bit X25519 identity key material from BIP-39 seed.
 */
expect object Argon2Kmp {
    fun deriveKey(
        seed: ByteArray,
        salt: ByteArray = "pmsg-v1-identity-seed".encodeToByteArray(),
        iterations: Int = 3,
        memoryKiB: Int = 32768,
        parallelism: Int = 1,
        outputLength: Int = 32
    ): ByteArray
}
