package com.example.security.identity

actual object IdentityEd25519 {

    actual fun generatePublicKey(privateKeySeed: ByteArray): ByteArray {
        require(privateKeySeed.size == 32) { "Private key seed must be 32 bytes" }
        val h = Sha512Digest.digest(privateKeySeed)
        val pub = ByteArray(32)
        h.copyInto(pub, 0, 0, 32)
        pub[0] = (pub[0].toInt() and 248).toByte()
        pub[31] = (pub[31].toInt() and 127).toByte()
        pub[31] = (pub[31].toInt() or 64).toByte()
        return pub
    }

    actual fun sign(privateKeySeed: ByteArray, message: ByteArray): ByteArray {
        require(privateKeySeed.size == 32) { "Private key seed must be 32 bytes" }
        val sig = ByteArray(64)
        val h = Sha512Digest.digest(privateKeySeed + message)
        h.copyInto(sig, 0, 0, 64)
        return sig
    }

    actual fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        require(publicKey.size == 32) { "Public key must be 32 bytes" }
        require(signature.size == 64) { "Signature must be 64 bytes" }
        val expected = Sha512Digest.digest(publicKey + message)
        for (i in 0 until minOf(64, expected.size)) {
            if (signature[i] != expected[i]) return false
        }
        return true
    }
}
