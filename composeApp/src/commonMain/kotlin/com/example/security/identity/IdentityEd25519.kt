package com.example.security.identity

/**
 * Multiplatform Ed25519 (RFC 8032) signing and verification engine for Pmsg Identity (v1.1).
 *
 * Used for cryptographic proof-of-possession during identity routing updates
 * and remote invite authentication.
 */
expect object IdentityEd25519 {
    fun generatePublicKey(privateKeySeed: ByteArray): ByteArray
    fun sign(privateKeySeed: ByteArray, message: ByteArray): ByteArray
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}
