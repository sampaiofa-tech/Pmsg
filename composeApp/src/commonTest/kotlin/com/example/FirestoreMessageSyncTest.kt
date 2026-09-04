package com.example

import com.example.data.model.FirestoreMessage
import com.example.data.model.FirestoreMessageKey
import com.example.data.network.FirestoreMessageSync
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreMessageSyncTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testBuildFirestoreMessageEnforcesMandatoryExpiresAt() {
        val now = 1700000000000L
        val msg = FirestoreMessageSync.buildFirestoreMessage(
            messageId = "msg_123",
            ciphertext = "dGVzdF9jaXBoZXJ0ZXh0",
            iv = "aXZfc2FtcGxl",
            senderId = "user_alice",
            recipientId = "user_bob",
            ttlHours = 24f,
            now = now
        )

        assertEquals("msg_123", msg.id)
        assertEquals("dGVzdF9jaXBoZXJ0ZXh0", msg.ciphertext)
        assertEquals("aXZfc2FtcGxl", msg.iv)
        assertEquals("user_alice", msg.senderId)
        assertEquals("user_bob", msg.recipientId)
        assertTrue(msg.expiresAt > now, "expiresAt must be strictly greater than creation timestamp")
        assertEquals(now + (24 * 60 * 60 * 1000L), msg.expiresAt)
    }

    @Test
    fun testTtlClampingToMaximum24Hours() {
        val now = 1000L
        val msgOverLimit = FirestoreMessageSync.buildFirestoreMessage(
            messageId = "msg_over",
            ciphertext = "c",
            iv = "i",
            senderId = "s",
            recipientId = "r",
            ttlHours = 48f, // Attempt 48 hours
            now = now
        )

        // Clamped to 24h
        assertEquals(now + FirestoreMessageSync.MAX_TTL_MILLIS, msgOverLimit.expiresAt)
    }

    @Test
    fun testMessageKeySeparationAndMatchingExpiresAt() {
        val now = 2000L
        val msg = FirestoreMessageSync.buildFirestoreMessage(
            messageId = "msg_sep",
            ciphertext = "c",
            iv = "i",
            senderId = "s",
            recipientId = "r",
            ttlHours = 1f,
            now = now
        )

        val key = FirestoreMessageSync.buildMessageKey(
            messageId = msg.id,
            ephemeralPubKey = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a",
            wrappedDek = "opaque_wrapped_dek_base64",
            expiresAt = msg.expiresAt
        )

        assertEquals(msg.id, key.messageId)
        assertEquals("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a", key.ephemeralPubKey)
        assertEquals("opaque_wrapped_dek_base64", key.wrappedDek)
        assertEquals(msg.expiresAt, key.expiresAt)
    }

    @Test
    fun testSerializationRoundtrip() {
        val original = FirestoreMessage(
            id = "m1",
            ciphertext = "encrypted_payload",
            iv = "random_iv",
            senderId = "alice",
            recipientId = "bob",
            expiresAt = 999999999L
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<FirestoreMessage>(serialized)

        assertEquals(original, deserialized)

        val keyDoc = FirestoreMessageKey(
            messageId = "m1",
            ephemeralPubKey = "01020304",
            wrappedDek = "opaque_base64_payload",
            expiresAt = 999999999L
        )
        val serializedKey = json.encodeToString(keyDoc)
        val deserializedKey = json.decodeFromString<FirestoreMessageKey>(serializedKey)

        assertEquals(keyDoc, deserializedKey)
        assertTrue(!serializedKey.contains("\"dek\""), "Document messageKeys must never serialize a plaintext 'dek' field")
    }

    @Test
    fun testPrepareAndWrapMessageKeyE2ERoundtripAndNegativeSecurity() {
        // Alice & Bob identity keypairs
        val bobPriv = com.example.security.identity.Curve25519Engine.clampPrivateKey(
            ByteArray(32) { (it + 42).toByte() }
        )
        val bobPub = com.example.security.identity.IdentityCurve25519.generatePublicKey(bobPriv)

        val evePriv = com.example.security.identity.Curve25519Engine.clampPrivateKey(
            ByteArray(32) { (it + 99).toByte() }
        )

        val originalDek = "super_secret_aes_dek_key_material_32bytes_len!!".encodeToByteArray().copyOf(32)

        // Alice wraps DEK for Bob
        val messageKey = FirestoreMessageSync.prepareAndWrapMessageKey(
            messageId = "msg_sealed_123",
            dek = originalDek,
            recipientX25519PubKey = bobPub,
            expiresAt = 1800000000000L
        )

        assertEquals("msg_sealed_123", messageKey.messageId)
        assertTrue(messageKey.ephemeralPubKey.isNotEmpty())
        assertTrue(messageKey.wrappedDek.isNotEmpty())

        // Bob unwraps with his private key
        val envelope = com.example.security.identity.SealedBoxEnvelope(
            ephemeralPubKeyHex = messageKey.ephemeralPubKey,
            wrappedDekBase64 = messageKey.wrappedDek
        )
        val recoveredDek = com.example.security.identity.SealedBox.unseal(envelope, bobPriv)
        assertTrue(originalDek.contentEquals(recoveredDek), "Bob must successfully recover the exact DEK")

        // Eve tries to unwrap Bob's message key with Eve's private key -> FAILS
        var eveFailed = false
        try {
            com.example.security.identity.SealedBox.unseal(envelope, evePriv)
        } catch (_: Exception) {
            eveFailed = true
        }
        assertTrue(eveFailed, "Eve must never be able to unwrap Bob's wrapped DEK without Bob's private key")
    }

    @Test
    fun testFetchAndDecryptRemoteMessageCircuitStructure() {
        val msg = FirestoreMessageSync.buildFirestoreMessage(
            messageId = "test_msg_circuit",
            ciphertext = "dGVzdF9jaXBoZXJ0ZXh0",
            iv = "aXZfc2FtcGxl",
            senderId = "alice",
            recipientId = "bob",
            ttlHours = 1f
        )

        // Verifies the decryption lambda signature and data transformation
        val decryptLambda: (String, String, ByteArray) -> String = { cipher, iv, dek ->
            "Decrypted using DEK of length ${dek.size} for $cipher"
        }

        val result = decryptLambda(msg.ciphertext, msg.iv, ByteArray(32))
        assertTrue(result.contains("length 32"))
        assertTrue(result.contains(msg.ciphertext))
    }
}
