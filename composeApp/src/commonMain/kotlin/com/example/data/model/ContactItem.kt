package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * Domain representation of a Contact in Pmsg.
 * Display name is in plaintext in memory, but NEVER on disk (stored as encrypted envelope).
 */
@Serializable
data class ContactItem(
    val fingerprint: String,
    val pubKey: String,
    val currentAuthUid: String,
    val displayName: String,
    val securityNumber: String,
    val verified: Boolean = false,
    val addedAt: Long = 0L
)
