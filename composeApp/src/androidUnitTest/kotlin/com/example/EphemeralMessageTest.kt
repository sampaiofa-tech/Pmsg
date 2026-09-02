package com.example

import com.example.data.model.BurnerChannel
import com.example.data.model.EphemeralMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EphemeralMessageCommonTest {

    @Test
    fun testMessageExpirationCalculation() {
        val now = 1000000L
        val ttl24hMillis = 24 * 60 * 60 * 1000L
        val msg = EphemeralMessage(
            roomId = "test_room",
            senderId = "ME",
            senderName = "Você",
            content = "ENC:test",
            timestamp = now,
            expiresAt = now + ttl24hMillis
        )

        // Fresh message
        assertFalse(msg.isExpired(now))
        assertEquals(ttl24hMillis, msg.remainingMillis(now))
        assertEquals(0f, msg.expirationProgress(now), 0.01f)

        // Halfway
        val halfway = now + (ttl24hMillis / 2)
        assertFalse(msg.isExpired(halfway))
        assertEquals(ttl24hMillis / 2, msg.remainingMillis(halfway))
        assertEquals(0.5f, msg.expirationProgress(halfway), 0.01f)

        // Expired
        val expiredTime = now + ttl24hMillis + 1000L
        assertTrue(msg.isExpired(expiredTime))
        assertEquals(0L, msg.remainingMillis(expiredTime))
        assertEquals(1.0f, msg.expirationProgress(expiredTime), 0.01f)
    }

    @Test
    fun testVanishAfterReadBehavior() {
        val now = 1000000L
        val msg = EphemeralMessage(
            roomId = "test_room",
            senderId = "CONTACT",
            senderName = "Alice",
            content = "ENC:secret",
            timestamp = now,
            expiresAt = now + 86400000L,
            isRead = true,
            readAt = now,
            disappearAfterReadSeconds = 10
        )

        // 5 seconds after read
        val after5s = now + 5000L
        assertFalse(msg.isExpired(after5s))
        assertEquals(5000L, msg.remainingMillis(after5s))
        assertEquals(0.5f, msg.expirationProgress(after5s), 0.01f)

        // 11 seconds after read (expired)
        val after11s = now + 11000L
        assertTrue(msg.isExpired(after11s))
        assertEquals(0L, msg.remainingMillis(after11s))
        assertEquals(1.0f, msg.expirationProgress(after11s), 0.01f)
    }

    @Test
    fun testShreddedMessageIsInstantlyExpired() {
        val now = 1000000L
        val msg = EphemeralMessage(
            roomId = "test_room",
            senderId = "ME",
            senderName = "Você",
            content = "*** PURGED ***",
            timestamp = now,
            expiresAt = now + 86400000L,
            isShredded = true
        )

        assertTrue(msg.isExpired(now))
    }

    @Test
    fun testBurnerChannelFormattedRemainingTime() {
        val now = 1000000L
        val channel = BurnerChannel(
            id = "test_ch",
            name = "Test Channel",
            lastMessageTimestamp = now,
            defaultTtlHours = 2f
        )

        val formatted = channel.formattedRemainingTime(now)
        assertEquals("02h 00m", formatted)

        val halfway = now + (60 * 60 * 1000L) // 1h later
        assertEquals("01h 00m", channel.formattedRemainingTime(halfway))

        val expired = now + (3 * 60 * 60 * 1000L)
        assertEquals("00:00", channel.formattedRemainingTime(expired))
    }

    @Test
    fun testSerializationRoundTrip() {
        val msg = EphemeralMessage(
            roomId = "ch_123",
            senderId = "ME",
            senderName = "Você",
            content = "Mensagem ultra secreta",
            ttlOptionHours = 1f
        )

        val json = Json.encodeToString(msg)
        val decoded = Json.decodeFromString<EphemeralMessage>(json)
        assertEquals(msg.roomId, decoded.roomId)
        assertEquals(msg.content, decoded.content)
    }
}
