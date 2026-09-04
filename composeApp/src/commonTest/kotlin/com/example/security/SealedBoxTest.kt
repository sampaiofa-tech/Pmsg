package com.example.security

import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.SealedBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SealedBoxTest {

    @Test
    fun testSealedBoxRoundtripAliceToBob() {
        val alice = IdentityCryptoManager.generateNewIdentity()
        val bob = IdentityCryptoManager.generateNewIdentity()

        val secretDek = "pmsg_ephemeral_dek_secret_key_32_bytes_len!!".encodeToByteArray()

        // Alice seals DEK using Bob's public key
        val envelope = SealedBox.seal(
            dek = secretDek,
            recipientPubKey = bob.keyPair.publicKey
        )

        assertTrue(envelope.ephemeralPubKeyHex.length == 64, "Ephemeral public key must be 32 bytes (64 hex characters)")
        assertTrue(envelope.wrappedDekBase64.isNotBlank(), "Wrapped DEK must not be empty")

        // Bob unseals using his private key
        val recoveredDek = SealedBox.unseal(
            envelope = envelope,
            recipientPrivKey = bob.keyPair.privateKey
        )

        assertEquals(
            secretDek.decodeToString(),
            recoveredDek.decodeToString(),
            "Bob must recover the exact original DEK plaintext"
        )
    }

    @Test
    fun testNegativeAuthorizationEveCannotUnsealBobsDek() {
        val bob = IdentityCryptoManager.generateNewIdentity()
        val eve = IdentityCryptoManager.generateNewIdentity() // Attacker

        val secretDek = "classified_military_ephemeral_data".encodeToByteArray()

        // Sealed specifically for Bob
        val envelope = SealedBox.seal(
            dek = secretDek,
            recipientPubKey = bob.keyPair.publicKey
        )

        // Eve tries to unseal with her own private key
        assertFails("Eve must fail to unseal Bob's wrapped DEK") {
            SealedBox.unseal(
                envelope = envelope,
                recipientPrivKey = eve.keyPair.privateKey
            )
        }
    }

    @Test
    fun testDeterministicWrappingWithFixedEphemeralKeyAndNonce() {
        val bob = IdentityCryptoManager.generateNewIdentity()
        val fixedEphemeralPriv = ByteArray(32) { (it + 1).toByte() }
        val fixedNonce = ByteArray(12) { (it + 7).toByte() }
        val dek = "constant_dek_for_determinism_chk".encodeToByteArray()

        val envelope1 = SealedBox.seal(
            dek = dek,
            recipientPubKey = bob.keyPair.publicKey,
            customEphemeralPriv = fixedEphemeralPriv,
            customNonce = fixedNonce
        )

        val envelope2 = SealedBox.seal(
            dek = dek,
            recipientPubKey = bob.keyPair.publicKey,
            customEphemeralPriv = fixedEphemeralPriv,
            customNonce = fixedNonce
        )

        assertEquals(envelope1.ephemeralPubKeyHex, envelope2.ephemeralPubKeyHex)
        assertEquals(envelope1.wrappedDekBase64, envelope2.wrappedDekBase64)

        // Both unseal correctly
        val recovered = SealedBox.unseal(envelope1, bob.keyPair.privateKey)
        assertEquals(dek.decodeToString(), recovered.decodeToString())
    }
}
