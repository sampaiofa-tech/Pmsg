package com.example.data.network

import com.example.data.model.FirestoreMessage
import com.example.data.model.FirestoreMessageKey

/**
 * Helper to prepare and serialize messages for server-side Firestore synchronization.
 *
 * Enforces:
 * 1. Mandatory `expiresAt` (Timestamp) for every ephemeral message (defaulting to max TTL 24h).
 * 2. Separation of ciphertext and DEK (Data Encryption Key) into separate collections.
 * 3. Strict schema adherence: { ciphertext, iv, senderId, recipientId, expiresAt }.
 */
object FirestoreMessageSync {

    const val MAX_TTL_MILLIS: Long = 24 * 60 * 60 * 1000L // 24 hours
    const val MIN_TTL_MILLIS: Long = 10 * 1000L // 10 seconds

    fun buildFirestoreMessage(
        messageId: String,
        ciphertext: String,
        iv: String,
        senderId: String,
        recipientId: String,
        ttlHours: Float = 24f,
        now: Long = System.currentTimeMillis()
    ): FirestoreMessage {
        val ttlMillis = (ttlHours * 60 * 60 * 1000L).toLong()
            .coerceIn(MIN_TTL_MILLIS, MAX_TTL_MILLIS)
        val expiresAt = now + ttlMillis

        return FirestoreMessage(
            id = messageId,
            ciphertext = ciphertext,
            iv = iv,
            senderId = senderId,
            recipientId = recipientId,
            expiresAt = expiresAt
        )
    }

    fun buildMessageKey(
        messageId: String,
        dek: String,
        expiresAt: Long
    ): FirestoreMessageKey {
        return FirestoreMessageKey(
            messageId = messageId,
            dek = dek,
            expiresAt = expiresAt
        )
    }

    /**
     * Circuito de Leitura Autorizada:
     * Destinatário autenticado invoca KeyStoreClient.getMessageKey para obter a DEK,
     * e decripta o ciphertext de trânsito em memória volátil.
     */
    suspend fun fetchAndDecryptRemoteMessage(
        message: FirestoreMessage,
        idToken: String,
        decryptPayload: (ciphertext: String, iv: String, dek: String) -> String
    ): Result<String> {
        val keyResult = KeyStoreClient.getMessageKey(message.id, idToken)
        if (!keyResult.success || keyResult.dek == null) {
            return Result.failure(
                IllegalStateException(keyResult.errorMessage ?: "Falha ao obter DEK autorizada do servidor.")
            )
        }

        return try {
            val decrypted = decryptPayload(message.ciphertext, message.iv, keyResult.dek)
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
