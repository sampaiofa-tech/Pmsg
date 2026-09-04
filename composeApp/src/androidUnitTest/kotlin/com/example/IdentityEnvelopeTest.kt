package com.example

import com.example.security.identity.IdentityCryptoManager
import com.example.util.security.CryptoManager
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IdentityEnvelopeTest {

    @Before
    fun setup() {
        CryptoManager.resetForTesting()
    }

    @Test
    fun testEnvelopeEncryptionRoundTrip() {
        val sampleData = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val encrypted = IdentityCryptoManager.envelopeEncrypt(sampleData)
        val decrypted = IdentityCryptoManager.envelopeDecrypt(encrypted)

        assertTrue(sampleData.contentEquals(decrypted))
    }

    @Test
    fun testMnemonic_Provisioning_ZeroPlaintextStorage_AndReview() {
        com.example.security.identity.IdentityManager.clearIdentity()
        org.junit.Assert.assertFalse(com.example.security.identity.IdentityManager.hasIdentity())

        val draft = com.example.security.identity.IdentityManager.provisionNewIdentity()
        org.junit.Assert.assertEquals(12, draft.mnemonic.size)

        // Confirm and save to KeyVault
        com.example.security.identity.IdentityManager.confirmAndSaveIdentity(draft)
        org.junit.Assert.assertTrue(com.example.security.identity.IdentityManager.hasIdentity())

        // Zero plaintext check: Mnemonic MUST NOT appear in storage
        val stored = com.example.security.identity.IdentityStorage.getIdentity()
        org.junit.Assert.assertNotNull(stored)
        val allWords = draft.mnemonic.joinToString(" ")
        org.junit.Assert.assertFalse(stored!!.publicKeyBase64.contains(allWords))
        org.junit.Assert.assertFalse(stored.fingerprintHex.contains(allWords))
        org.junit.Assert.assertFalse(stored.safetyNumber.contains(allWords))

        // Review with biometrics (retrieves encrypted entropy and derives mnemonic on the fly)
        val reviewed = com.example.security.identity.IdentityManager.getMnemonicWords()
        org.junit.Assert.assertNotNull(reviewed)
        org.junit.Assert.assertEquals(draft.mnemonic, reviewed)
    }
}
