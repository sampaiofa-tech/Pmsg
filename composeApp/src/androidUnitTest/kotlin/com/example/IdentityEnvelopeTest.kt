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
}
