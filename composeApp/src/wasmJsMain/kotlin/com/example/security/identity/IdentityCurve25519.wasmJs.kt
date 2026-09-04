package com.example.security.identity

actual object IdentityCurve25519 {
    actual fun generatePublicKey(privateKey: ByteArray): ByteArray {
        return Curve25519Engine.scalarMultBase(privateKey)
    }

    actual fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        return Curve25519Engine.scalarMult(myPrivateKey, peerPublicKey)
    }
}
