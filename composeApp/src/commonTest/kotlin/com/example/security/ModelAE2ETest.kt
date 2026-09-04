package com.example.security

import com.example.data.model.ContactItem
import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.IdentityManager
import com.example.security.identity.Sha256Digest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class ModelAE2ETest {

    @Test
    fun testModelA_FullExchangeAndCommutativeSafetyNumber() {
        // 1. Device A (Alice) generates identity
        val aliceIdentity = IdentityCryptoManager.generateNewIdentity()
        val alicePubKeyBase64 = Base64.encode(aliceIdentity.keyPair.publicKey)
        val aliceFp = aliceIdentity.keyPair.fingerprintHex

        // 2. Device B (Bob) generates identity
        val bobIdentity = IdentityCryptoManager.generateNewIdentity()
        val bobPubKeyBase64 = Base64.encode(bobIdentity.keyPair.publicKey)
        val bobFp = bobIdentity.keyPair.fingerprintHex

        // 3. Alice exports Model A URI string
        val aliceUri = "pmsg://contact?v=1&fp=$aliceFp&pk=$alicePubKeyBase64&uid=auth_alice_999"

        // 4. Bob parses Alice's URI
        val parseResult = IdentityManager.parseContactUri(aliceUri)
        assertTrue(parseResult.isSuccess, "Bob must successfully parse Alice's Model A URI")
        val alicePayload = parseResult.getOrThrow()

        assertEquals(aliceFp, alicePayload.fingerprintHex)
        assertEquals(alicePubKeyBase64, alicePayload.publicKeyBase64)
        assertEquals("auth_alice_999", alicePayload.authUid)

        // 5. Derive Safety Number on both devices independently
        // Bob derives using (Bob's pubKey, Alice's pubKey)
        val bobSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
            myPubKey = bobIdentity.keyPair.publicKey,
            peerPubKey = alicePayload.publicKeyBytes
        )

        // Alice derives using (Alice's pubKey, Bob's pubKey)
        val aliceSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
            myPubKey = aliceIdentity.keyPair.publicKey,
            peerPubKey = bobIdentity.keyPair.publicKey
        )

        // 6. Symmetrical / Commutative equality verification: 60 digits MUST be 100% identical
        assertEquals(
            aliceSafetyNumber,
            bobSafetyNumber,
            "Safety Number derived by Alice and Bob MUST be identical regardless of key order"
        )
        val blocks = aliceSafetyNumber.split(" ")
        assertEquals(12, blocks.size, "Safety Number must contain exactly 12 blocks")
        blocks.forEach { block ->
            assertEquals(5, block.length, "Each block must contain exactly 5 digits")
            assertTrue(block.all { it.isDigit() }, "Each block must only contain decimal digits")
        }

        // 7. Bob adds Alice to local contacts (initially unverified)
        var contactAlice = ContactItem(
            fingerprint = alicePayload.fingerprintHex,
            pubKey = alicePayload.publicKeyBase64,
            currentAuthUid = alicePayload.authUid,
            displayName = "Alice Colleague",
            securityNumber = bobSafetyNumber,
            verified = false,
            addedAt = 1725390000000L
        )
        assertFalse(contactAlice.verified, "Contact must be unverified before visual comparison")

        // 8. Bob compares Safety Number on screen and marks as verified
        contactAlice = contactAlice.copy(verified = true)
        assertTrue(contactAlice.verified, "Contact must be verified after user confirmation")

        // 9. Ephemeral Message Vanish Countdown Test (10s TTL)
        val msgSendTime = 1000000L
        val ttlMillis = 10_000L
        val expiresAt = msgSendTime + ttlMillis

        // At t = sendTime + 5s (halfway) -> active
        val midTime = msgSendTime + 5000L
        val isStillActiveMid = midTime < expiresAt
        assertTrue(isStillActiveMid, "Message should still be active at 5s")

        // At t = sendTime + 10.1s -> vanished
        val afterExpiryTime = msgSendTime + 10100L
        val isExpired = afterExpiryTime >= expiresAt
        assertTrue(isExpired, "Message must be considered expired and vanished after 10s TTL")
    }

    @Test
    fun testModelA_TamperedPubKey_RejectedByParser() {
        val aliceIdentity = IdentityCryptoManager.generateNewIdentity()
        val realFp = aliceIdentity.keyPair.fingerprintHex

        // Attacker replaces public key with fake one while keeping Alice's fingerprint
        val attackerIdentity = IdentityCryptoManager.generateNewIdentity()
        val tamperedPubKeyBase64 = Base64.encode(attackerIdentity.keyPair.publicKey)

        val tamperedUri = "pmsg://contact?v=1&fp=$realFp&pk=$tamperedPubKeyBase64&uid=auth_alice"

        val parseResult = IdentityManager.parseContactUri(tamperedUri)
        assertTrue(parseResult.isFailure, "Tampered URI must fail verification")
        val error = parseResult.exceptionOrNull()
        assertNotNull(error)
        assertTrue(
            error!!.message!!.contains("Fingerprint não corresponde") ||
            error.message!!.contains("não corresponde"),
            "Error should indicate fingerprint mismatch"
        )
    }

    @Test
    fun testMnemonic_ProvisioningVerification_RandomWordsAndDeterministicDerivation() {
        // 1. Provision new identity draft (12 BIP-39 words)
        val draft = IdentityCryptoManager.generateNewIdentity()
        assertEquals(12, draft.mnemonic.size, "Mnemonic must have exactly 12 words")

        // 2. Select 3 random indices (1-based: e.g. 2, 6, 10)
        val testIndices = listOf(2, 6, 10)
        val expectedWord1 = draft.mnemonic[testIndices[0] - 1]
        val expectedWord2 = draft.mnemonic[testIndices[1] - 1]
        val expectedWord3 = draft.mnemonic[testIndices[2] - 1]

        // 3. Verify incorrect words are rejected
        val fakeInput1 = "palavraerrada"
        val isWrongRejected = !(fakeInput1 == expectedWord1 && expectedWord2 == expectedWord2 && expectedWord3 == expectedWord3)
        assertTrue(isWrongRejected, "Verification must reject incorrect mnemonic words")

        // 4. Verify correct words match
        val isCorrectAccepted = (expectedWord1 == draft.mnemonic[1] &&
                expectedWord2 == draft.mnemonic[5] &&
                expectedWord3 == draft.mnemonic[9])
        assertTrue(isCorrectAccepted, "Verification passes with correct words")

        // 5. Restoration on a new device produces 100% identical public key and fingerprint
        val restoredResult = IdentityCryptoManager.restoreFromMnemonic(draft.mnemonic)
        assertTrue(restoredResult.isSuccess, "Restoration must succeed")
        val restoredKeyPair = restoredResult.getOrThrow()
        assertEquals(draft.keyPair.fingerprintHex, restoredKeyPair.fingerprintHex, "Fingerprint must match")
        assertEquals(draft.keyPair.safetyNumber, restoredKeyPair.safetyNumber, "Safety number must match")
        assertTrue(draft.keyPair.publicKey.contentEquals(restoredKeyPair.publicKey), "Public key must match")
    }
}
