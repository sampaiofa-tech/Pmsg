package com.example

import com.example.data.model.EphemeralMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

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

        // 5 seconds after read (not yet expired)
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

        assertTrue("Shredded message must immediately report expired", msg.isExpired(now))
    }
}
