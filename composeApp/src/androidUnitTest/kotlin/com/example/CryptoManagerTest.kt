package com.example

import com.example.util.security.CryptoManager
import com.example.util.security.SecurePrefsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CryptoManagerTest {

    @Before
    fun setup() {
        CryptoManager.resetForTesting()
    }

    @Test
    fun test512BitEncryptionAndDecryptionMatches() {
        val originalText = "TopSecretEphemeralPayload_12345"
        val encrypted = CryptoManager.encrypt(originalText)

        // Verify that the payload was actually encrypted with 512-bit header
        assertNotEquals(originalText, encrypted)
        assertTrue("Ciphertext should start with ENC512: prefix", encrypted.startsWith("ENC512:"))

        // Verify that decryption returns the exact original plaintext
        val decrypted = CryptoManager.decrypt(encrypted)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testEmptyPayloadHandling() {
        assertEquals("", CryptoManager.encrypt(""))
        assertEquals("", CryptoManager.decrypt(""))
    }

    @Test
    fun testLegacyPlaintextPassThrough() {
        val legacyText = "Mensagem em texto plano sem prefixo"
        val decrypted = CryptoManager.decrypt(legacyText)
        assertEquals(legacyText, decrypted)
    }

    @Test
    fun testCorruptedCiphertextHandling() {
        val corruptedPayload = "ENC512:VGhpcyBJcyBDb3JydXB0ZWREYXRhMTIz"
        val decrypted = CryptoManager.decrypt(corruptedPayload)
        assertTrue("Corrupted ciphertext should yield secure fallback", decrypted.contains("Criptografada"))
    }

    @Test
    fun testCryptoShreddingKeyInvalidation() {
        val originalText = "SuperConfidentialZeroTraceMessage"
        val encrypted = CryptoManager.encrypt(originalText)

        // Trigger crypto-shredding (master key rotation of both 256-bit keys)
        CryptoManager.invalidateAndRecreateMasterKey()

        // Decryption with new keys MUST fail on the old ciphertext
        val decryptedAfterShred = CryptoManager.decrypt(encrypted)
        assertNotEquals("Old ciphertext must be unrecoverable after crypto-shredding", originalText, decryptedAfterShred)
        assertTrue(decryptedAfterShred.contains("Criptografada"))
    }

    @Test
    fun testSecureNoiseGeneration() {
        val noise1 = CryptoManager.generateSecureNoise(32)
        val noise2 = CryptoManager.generateSecureNoise(32)

        assertTrue(noise1.isNotBlank())
        assertTrue(noise2.isNotBlank())
        assertNotEquals("Secure noise outputs must be cryptographically distinct", noise1, noise2)
    }

    @Test
    fun testZeroizeByteArray() {
        val sensitiveBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        CryptoManager.zeroize(sensitiveBytes)
        for (b in sensitiveBytes) {
            assertEquals("Memory byte must be zeroed out", 0.toByte(), b)
        }
    }

    @Test
    fun testZeroizeCharArray() {
        val sensitiveChars = charArrayOf('p', 'a', 's', 's', 'w', 'o', 'r', 'd')
        CryptoManager.zeroize(sensitiveChars)
        for (c in sensitiveChars) {
            assertEquals("Memory char must be zeroed out", '\u0000', c)
        }
    }

    @Test
    fun testPbkdf2PinDerivation() {
        val pin = "9876"
        val salt = "1234567890abcdef"
        val hash1 = SecurePrefsHelper.hashPinPbkdf2(pin, salt)
        val hash2 = SecurePrefsHelper.hashPinPbkdf2(pin, salt)

        assertEquals("Same PIN and salt must yield identical hash", hash1, hash2)
        assertTrue("Hash must be formatted as PBKDF2 string", hash1.startsWith("PBKDF2$"))

        val differentSaltHash = SecurePrefsHelper.hashPinPbkdf2(pin, "fedcba0987654321")
        assertNotEquals("Different salts must yield different hashes", hash1, differentSaltHash)
    }
}
