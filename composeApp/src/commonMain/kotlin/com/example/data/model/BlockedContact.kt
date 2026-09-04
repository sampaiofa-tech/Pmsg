package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * Domain representation of a blocked contact in Pmsg.
 * Maintained client-side in encrypted local storage.
 * The server remains zero-knowledge regarding the local blocklist.
 */
@Serializable
data class BlockedContact(
    val fingerprint: String,
    val blockedAt: Long = 0L
)
