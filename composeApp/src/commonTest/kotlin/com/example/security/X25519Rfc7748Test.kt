package com.example.security

import com.example.security.identity.Curve25519Engine
import com.example.security.identity.HkdfSha256
import com.example.security.identity.IdentityCurve25519
import kotlin.test.Test
import kotlin.test.assertEquals

class X25519Rfc7748Test {

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "")
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            result[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return result
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    @Test
    fun testRfc7748Section52AliceAndBobVectors() {
        val alicePriv = hexToBytes("77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a")
        val alicePubExpected = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a"

        val bobPriv = hexToBytes("5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb")
        val bobPubExpected = "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f"

        val sharedSecretExpected = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742"

        // 1. Test IdentityCurve25519 (Target Implementation)
        val alicePub = IdentityCurve25519.generatePublicKey(alicePriv)
        assertEquals(alicePubExpected, bytesToHex(alicePub), "IdentityCurve25519 Alice public key must match RFC 7748 §5.2")

        val bobPub = IdentityCurve25519.generatePublicKey(bobPriv)
        assertEquals(bobPubExpected, bytesToHex(bobPub), "IdentityCurve25519 Bob public key must match RFC 7748 §5.2")

        val aliceShared = IdentityCurve25519.computeSharedSecret(alicePriv, bobPub)
        assertEquals(sharedSecretExpected, bytesToHex(aliceShared), "IdentityCurve25519 Alice shared secret must match RFC 7748 §5.2")

        val bobShared = IdentityCurve25519.computeSharedSecret(bobPriv, alicePub)
        assertEquals(sharedSecretExpected, bytesToHex(bobShared), "IdentityCurve25519 Bob shared secret must match RFC 7748 §5.2")
    }

    @Test
    fun testHkdfRfc5869TestCase1() {
        val ikm = hexToBytes("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b")
        val salt = hexToBytes("000102030405060708090a0b0c")
        val info = hexToBytes("f0f1f2f3f4f5f6f7f8f9")
        val expectedPrk = "077709362c2e32df0ddc3f0dc47bba6390b6c73bb50f9c3122ec844ad7c2b3e5"
        val expectedOkm = "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"

        val prk = HkdfSha256.extract(salt, ikm)
        assertEquals(expectedPrk, bytesToHex(prk), "HKDF-Extract PRK must match RFC 5869 Test Case 1")

        val okm = HkdfSha256.expand(prk, info, 42)
        assertEquals(expectedOkm, bytesToHex(okm), "HKDF-Expand OKM must match RFC 5869 Test Case 1")

        val directOkm = HkdfSha256.deriveKey(ikm, salt, info, 42)
        assertEquals(expectedOkm, bytesToHex(directOkm), "HKDF deriveKey must match RFC 5869 Test Case 1")
    }
}
