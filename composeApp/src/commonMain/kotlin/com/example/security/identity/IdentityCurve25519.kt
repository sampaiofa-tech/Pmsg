package com.example.security.identity

/**
 * Multiplatform X25519 (RFC 7748) operations for Pmsg Identity layer.
 */
expect object IdentityCurve25519 {
    fun generatePublicKey(privateKey: ByteArray): ByteArray
    fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray
}
