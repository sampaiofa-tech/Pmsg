package com.example

import com.example.util.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    @Before
    fun setup() {
        CryptoManager.resetForTesting()
    }

    @Test
    fun testEncryptionAndDecryptionMatches() {
        val originalText = "TopSecretEphemeralPayload_12345"
        val encrypted = CryptoManager.encrypt(originalText)

        // Verify that the payload was actually encrypted
        assertNotEquals(originalText, encrypted)
        assertTrue("Ciphertext should start with ENC: prefix", encrypted.startsWith("ENC:"))

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
        val corruptedPayload = "ENC:VGhpcyBJcyBDb3JydXB0ZWREYXRhMTIz"
        val decrypted = CryptoManager.decrypt(corruptedPayload)
        assertTrue("Corrupted ciphertext should yield secure fallback", decrypted.contains("Criptografada"))
    }

    @Test
    fun testCryptoShreddingKeyInvalidation() {
        val originalText = "SuperConfidentialZeroTraceMessage"
        val encrypted = CryptoManager.encrypt(originalText)

        // Trigger crypto-shredding (master key rotation)
        CryptoManager.invalidateAndRecreateMasterKey()

        // Decryption with new key MUST fail on the old ciphertext
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
}
