package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * Server-side Firestore document in "messages" collection.
 * Required schema: { ciphertext, iv, senderId, recipientId, expiresAt }
 *
 * All fields are strictly validated server-side by firestore.rules.
 * expiresAt must be a Timestamp and is mandatory for document creation.
 */
@Serializable
data class FirestoreMessage(
    val id: String = "",
    val ciphertext: String = "",
    val iv: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val expiresAt: Long = 0L // Epoch millis representing Firestore Timestamp
)

/**
 * Server-side Firestore document in "messageKeys" collection.
 * Required schema: { messageId, dek, expiresAt }
 *
 * Stored in a separate collection. Access rules:
 * allow read, write: if false; (Universal client block).
 * Only accessible by Cloud Functions (Admin SDK) for Crypto-Shredding.
 */
@Serializable
data class FirestoreMessageKey(
    val messageId: String = "",
    val dek: String = "",
    val expiresAt: Long = 0L // Must match exactly the message expiresAt
)
