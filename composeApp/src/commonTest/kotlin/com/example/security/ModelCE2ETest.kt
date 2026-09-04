package com.example.security

import com.example.data.network.IdentityNetworkClient
import com.example.security.identity.IdentityCryptoManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class ModelCE2ETest {

    @Test
    fun testModelC_InviteLinkParsing() {
        val sampleToken = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"
        val sampleFp = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"

        // 1. Direct 64-char hex token
        val parsedDirect = IdentityNetworkClient.parseInviteToken(sampleToken)
        assertEquals(sampleToken, parsedDirect)

        // 2. Full deep link: pmsg://invite?token=...&fp=...
        val fullUri = "pmsg://invite?token=$sampleToken&fp=$sampleFp"
        val parsedUri = IdentityNetworkClient.parseInviteToken(fullUri)
        assertEquals(sampleToken, parsedUri)

        // 3. Reversed query param order: pmsg://invite?fp=...&token=...
        val reversedUri = "pmsg://invite?fp=$sampleFp&token=$sampleToken"
        val parsedReversed = IdentityNetworkClient.parseInviteToken(reversedUri)
        assertEquals(sampleToken, parsedReversed)

        // 4. Invalid inputs
        assertNull(IdentityNetworkClient.parseInviteToken(""))
        assertNull(IdentityNetworkClient.parseInviteToken("short_token"))
        assertNull(IdentityNetworkClient.parseInviteToken("pmsg://invite?token=short"))
        assertNull(IdentityNetworkClient.parseInviteToken("pmsg://invite?other=123"))
    }

    @Test
    fun testModelC_InviteExchangeAndSafetyNumber() {
        // 1. Alice creates identity
        val aliceIdentity = IdentityCryptoManager.generateNewIdentity()
        val alicePubKeyBase64 = Base64.encode(aliceIdentity.keyPair.publicKey)
        val aliceFp = aliceIdentity.keyPair.fingerprintHex

        // 2. Bob creates identity
        val bobIdentity = IdentityCryptoManager.generateNewIdentity()
        val bobPubKeyBase64 = Base64.encode(bobIdentity.keyPair.publicKey)

        // 3. Simulate Modelo C exchange (Alice invites Bob)
        // Alice generates invite token
        val inviteToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val inviteLink = "pmsg://invite?token=$inviteToken&fp=$aliceFp"
        assertEquals(inviteToken, IdentityNetworkClient.parseInviteToken(inviteLink))

        // Bob accepts invite, receiving Alice's technical identity (fp and pubKey)
        val receivedPubKeyBytes = Base64.decode(alicePubKeyBase64)
        val computedFp = com.example.security.identity.Sha256Digest.digestHex(receivedPubKeyBytes)
        assertEquals(aliceFp, computedFp, "Bob verifies cryptographic binding between pubKey and fingerprint")

        // 4. Bob derives Pair Safety Number
        val bobSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
            myPubKey = bobIdentity.keyPair.publicKey,
            peerPubKey = receivedPubKeyBytes
        )

        // Alice derives Pair Safety Number with Bob's public key
        val aliceSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
            myPubKey = aliceIdentity.keyPair.publicKey,
            peerPubKey = bobIdentity.keyPair.publicKey
        )

        // 5. Commutative verification: must match exactly
        assertEquals(
            aliceSafetyNumber,
            bobSafetyNumber,
            "Safety Number derived remotely via Modelo C MUST be identical on both sides"
        )
        assertEquals(71, aliceSafetyNumber.length, "Safety Number must have 60 digits + 11 spaces = 71 characters")
    }

    @Test
    fun testModelC_RecoveryDeterminismAndSafetyNumberPreservation() {
        // 1. Alice generates identity on Device 1 (original)
        val aliceDevice1 = IdentityCryptoManager.generateNewIdentity()
        val mnemonic = aliceDevice1.mnemonic
        assertEquals(12, mnemonic.size)

        // 2. Bob generates identity
        val bobIdentity = IdentityCryptoManager.generateNewIdentity()

        // 3. Alice pairs with Bob from Device 1
        val safetyNumberDevice1 = IdentityCryptoManager.computePairSafetyNumber(
            myPubKey = aliceDevice1.keyPair.publicKey,
            peerPubKey = bobIdentity.keyPair.publicKey
        )

        // 4. Alice loses Device 1 and restores on Device 2 using the same 12 words
        val restoreResult = IdentityCryptoManager.restoreFromMnemonic(mnemonic)
        assertTrue(restoreResult.isSuccess, "Restoration must succeed on Device 2")
        val aliceDevice2 = restoreResult.getOrThrow()

        // 5. Assert 100% cryptographic equivalence
        assertEquals(aliceDevice1.keyPair.fingerprintHex, aliceDevice2.fingerprintHex)
        assertEquals(aliceDevice1.keyPair.safetyNumber, aliceDevice2.safetyNumber)
        assertTrue(aliceDevice1.keyPair.publicKey.contentEquals(aliceDevice2.publicKey))

        // 6. Assert Bob's safety number with Alice remains 100% valid after recovery
        val safetyNumberDevice2 = IdentityCryptoManager.computePairSafetyNumber(
            myPubKey = aliceDevice2.publicKey,
            peerPubKey = bobIdentity.keyPair.publicKey
        )
        assertEquals(
            safetyNumberDevice1,
            safetyNumberDevice2,
            "Safety Number with Bob MUST remain completely intact after recovery"
        )
    }
}
