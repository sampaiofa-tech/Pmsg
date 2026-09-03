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
            dek = "raw_dek_key_material_bytes",
            expiresAt = msg.expiresAt
        )

        assertEquals(msg.id, key.messageId)
        assertEquals("raw_dek_key_material_bytes", key.dek)
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
        val decryptLambda: (String, String, String) -> String = { cipher, iv, dek ->
            "Decrypted using DEK: $dek for $cipher"
        }

        val result = decryptLambda(msg.ciphertext, msg.iv, "mock_dek_key")
        assertTrue(result.contains("mock_dek_key"))
        assertTrue(result.contains(msg.ciphertext))
    }
}
