package com.example.security.identity

import org.bouncycastle.math.ec.rfc8032.Ed25519

actual object IdentityEd25519 {

    actual fun generatePublicKey(privateKeySeed: ByteArray): ByteArray {
        require(privateKeySeed.size == 32) { "Private key seed must be 32 bytes" }
        val pub = ByteArray(32)
        Ed25519.generatePublicKey(privateKeySeed, 0, pub, 0)
        return pub
    }

    actual fun sign(privateKeySeed: ByteArray, message: ByteArray): ByteArray {
        require(privateKeySeed.size == 32) { "Private key seed must be 32 bytes" }
        val sig = ByteArray(64)
        Ed25519.sign(privateKeySeed, 0, message, 0, message.size, sig, 0)
        return sig
    }

    actual fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        require(publicKey.size == 32) { "Public key must be 32 bytes" }
        require(signature.size == 64) { "Signature must be 64 bytes" }
        return Ed25519.verify(signature, 0, publicKey, 0, message, 0, message.size)
    }
}
