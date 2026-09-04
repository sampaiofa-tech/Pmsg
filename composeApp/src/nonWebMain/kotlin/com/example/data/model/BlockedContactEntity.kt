package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity representing a locally blocked contact in Pmsg.
 * Client-side only; the server operates zero-knowledge and has no
 * insight into user blocklists.
 */
@Serializable
@Entity(tableName = "blocked_contacts")
data class BlockedContactEntity(
    @PrimaryKey
    val fingerprint: String, // SHA-256(pubKey) hex 64 chars
    val blockedAt: Long = 0L // Timestamp epoch millis
) {
    fun toDomain(): BlockedContact = BlockedContact(
        fingerprint = fingerprint,
        blockedAt = blockedAt
    )
}
