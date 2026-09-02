package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Ephemeral message entity with strict TTL (Time-To-Live) expiration.
 * Supports text, photos, videos, files and burner notes.
 * Messages automatically expire after at most 24 hours (86,400,000 ms) or custom duration.
 * Once expired or incinerated, the record is permanently wiped with zero trace.
 */
@Serializable
@Entity(tableName = "ephemeral_messages")
data class EphemeralMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roomId: String,
    val senderId: String, // "ME" or contact ID
    val senderName: String,
    val content: String,
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L, // Default strictly 24 hours
    val ttlOptionHours: Float = 24f, // 24f, 12f, 6f, 1f, 0.083f (5 min), 0.0083f (30 sec), etc.
    val isEncrypted: Boolean = true,
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "FILE", "AUDIO", "BURNER_NOTE"
    val mediaUri: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val audioDurationSeconds: Int = 0,
    val isViewOnce: Boolean = false,
    val isViewed: Boolean = false,
    val viewedAt: Long? = null,
    val isDelivered: Boolean = true,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val disappearAfterReadSeconds: Int = 0, // 0 = standard TTL, > 0 = auto-vanishes X seconds after read
    val isShredded: Boolean = false
) {
    /**
     * Milliseconds remaining until self-destruction.
     */
    fun remainingMillis(now: Long): Long {
        if (disappearAfterReadSeconds > 0 && isRead && readAt != null) {
            val vanishAt = readAt + (disappearAfterReadSeconds * 1000L)
            return (minOf(expiresAt, vanishAt) - now).coerceAtLeast(0L)
        }
        return (expiresAt - now).coerceAtLeast(0L)
    }

    /**
     * Whether this message has expired.
     */
    fun isExpired(now: Long): Boolean {
        if (isShredded) return true
        if (disappearAfterReadSeconds > 0 && isRead && readAt != null) {
            val vanishAt = readAt + (disappearAfterReadSeconds * 1000L)
            if (now >= vanishAt) return true
        }
        return now >= expiresAt
    }

    /**
     * Expiration progress between 0.0f (freshly sent) to 1.0f (fully expired / burned).
     */
    fun expirationProgress(now: Long): Float {
        if (disappearAfterReadSeconds > 0 && isRead && readAt != null) {
            val vanishAt = readAt + (disappearAfterReadSeconds * 1000L)
            val totalDuration = (vanishAt - readAt).coerceAtLeast(1L)
            val elapsed = (now - readAt).coerceAtLeast(0L)
            return (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        }
        val totalDuration = (expiresAt - timestamp).coerceAtLeast(1L)
        val elapsed = (now - timestamp).coerceAtLeast(0L)
        return (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }
}
