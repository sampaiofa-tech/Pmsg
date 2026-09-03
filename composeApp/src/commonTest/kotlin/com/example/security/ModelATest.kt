package com.example.security

import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.IdentityManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class ModelATest {

    @Test
    fun testParseValidContactUri() {
        val identity = IdentityCryptoManager.generateNewIdentity()
        val pkBase64 = Base64.encode(identity.keyPair.publicKey)
        val uri = "pmsg://contact?v=1&fp=${identity.keyPair.fingerprintHex}&pk=$pkBase64&uid=user_alice_123"

        val result = IdentityManager.parseContactUri(uri)
        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()

        assertEquals(1, payload.version)
        assertEquals(identity.keyPair.fingerprintHex, payload.fingerprintHex)
        assertTrue(identity.keyPair.publicKey.contentEquals(payload.publicKeyBytes))
        assertEquals("user_alice_123", payload.authUid)
    }

    @Test
    fun testTamperedFingerprintRejected() {
        val identity = IdentityCryptoManager.generateNewIdentity()
        val pkBase64 = Base64.encode(identity.keyPair.publicKey)
        // Corrupt first char of fingerprint
        val corruptedFp = if (identity.keyPair.fingerprintHex.startsWith("a")) {
            "b" + identity.keyPair.fingerprintHex.substring(1)
        } else {
            "a" + identity.keyPair.fingerprintHex.substring(1)
        }
        val uri = "pmsg://contact?v=1&fp=$corruptedFp&pk=$pkBase64&uid=user_alice_123"

        val result = IdentityManager.parseContactUri(uri)
        assertTrue(result.isFailure, "Tampered fingerprint must fail validation")
        assertTrue(result.exceptionOrNull()?.message?.contains("Inconsistência criptográfica") == true)
    }

    @Test
    fun testInvalidSchemeRejected() {
        val uri = "https://example.com/contact?v=1&fp=abc&pk=123&uid=456"
        val result = IdentityManager.parseContactUri(uri)
        assertTrue(result.isFailure)
    }
}
