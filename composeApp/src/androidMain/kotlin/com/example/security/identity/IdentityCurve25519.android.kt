package com.example.security.identity

import org.bouncycastle.math.ec.rfc7748.X25519

actual object IdentityCurve25519 {

    actual fun generatePublicKey(privateKey: ByteArray): ByteArray {
        require(privateKey.size == 32) { "Private key must be 32 bytes" }
        val pubKey = ByteArray(32)
        X25519.scalarMultBase(privateKey, 0, pubKey, 0)
        return pubKey
    }

    actual fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        require(myPrivateKey.size == 32) { "Private key must be 32 bytes" }
        require(peerPublicKey.size == 32) { "Peer public key must be 32 bytes" }
        val sharedSecret = ByteArray(32)
        X25519.calculateAgreement(myPrivateKey, 0, peerPublicKey, 0, sharedSecret, 0)
        return sharedSecret
    }
}
