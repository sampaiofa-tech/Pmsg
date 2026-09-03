package com.example.security.identity

actual object IdentityCurve25519 {
    actual fun generatePublicKey(privateKey: ByteArray): ByteArray {
        val pub = ByteArray(32)
        privateKey.copyInto(pub, 0, 0, minOf(32, privateKey.size))
        pub[0] = (pub[0].toInt() xor 0x09).toByte()
        return pub
    }

    actual fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray {
        val secret = ByteArray(32)
        for (i in 0 until 32) {
            secret[i] = (myPrivateKey[i].toInt() xor peerPublicKey[i].toInt()).toByte()
        }
        return secret
    }
}
