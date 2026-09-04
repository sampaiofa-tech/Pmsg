package com.example.data.network

import com.example.data.model.FirestoreMessage
import com.example.data.model.FirestoreMessageKey
import com.example.security.identity.SealedBox
import com.example.security.identity.SealedBoxEnvelope

/**
 * Helper to prepare and serialize messages for server-side Firestore synchronization.
 *
 * Enforces:
 * 1. Mandatory `expiresAt` (Timestamp) for every ephemeral message (defaulting to max TTL 24h).
 * 2. Separation of ciphertext and DEK (Data Encryption Key) into separate collections.
 * 3. Zero-knowledge DEK wrapping: The server only sees opaque SealedBox envelopes (ephemeralPubKey, wrappedDek).
 * 4. Strict schema adherence: { ciphertext, iv, senderId, recipientId, expiresAt }.
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
        now: Long = PlatformEnvironment.currentTimeMillis()
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
        ephemeralPubKey: String,
        wrappedDek: String,
        expiresAt: Long
    ): FirestoreMessageKey {
        return FirestoreMessageKey(
            messageId = messageId,
            ephemeralPubKey = ephemeralPubKey,
            wrappedDek = wrappedDek,
            expiresAt = expiresAt
        )
    }

    /**
     * Helper to wrap a raw DEK for recipient using ephemeral SealedBox encryption.
     * Alice calls this before saving/sending the key to Firestore.
     */
    fun prepareAndWrapMessageKey(
        messageId: String,
        dek: ByteArray,
        recipientX25519PubKey: ByteArray,
        expiresAt: Long
    ): FirestoreMessageKey {
        val envelope = SealedBox.seal(dek = dek, recipientPubKey = recipientX25519PubKey)
        return FirestoreMessageKey(
            messageId = messageId,
            ephemeralPubKey = envelope.ephemeralPubKeyHex,
            wrappedDek = envelope.wrappedDekBase64,
            expiresAt = expiresAt
        )
    }

    /**
     * Circuito de Leitura Autorizada:
     * Destinatário autenticado invoca KeyStoreClient.getMessageKey para obter a DEK envelopada,
     * desembrulha usando SealedBox.unseal com a chave privada da identidade do destinatário (KeyVault),
     * e decripta o ciphertext de trânsito em memória volátil.
     */
    suspend fun fetchAndDecryptRemoteMessage(
        message: FirestoreMessage,
        recipientPrivKey: ByteArray,
        idToken: String,
        decryptPayload: (ciphertext: String, iv: String, dek: ByteArray) -> String
    ): Result<String> {
        val keyResult = KeyStoreClient.getMessageKey(message.id, idToken)
        if (!keyResult.success || keyResult.ephemeralPubKey == null || keyResult.wrappedDek == null) {
            return Result.failure(
                IllegalStateException(keyResult.errorMessage ?: "Falha ao obter DEK envelopada autorizada do servidor.")
            )
        }

        return try {
            val envelope = SealedBoxEnvelope(
                ephemeralPubKeyHex = keyResult.ephemeralPubKey,
                wrappedDekBase64 = keyResult.wrappedDek
            )
            val dek = SealedBox.unseal(envelope, recipientPrivKey)
            val decrypted = decryptPayload(message.ciphertext, message.iv, dek)
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enforçamento Client-Side da Blocklist no fetch de mensagens:
     * Mensagens de remetentes bloqueados são descartadas SEM decifração nem exibição (auto-purge),
     * com contagem local para auditoria.
     */
    suspend fun filterBlockedMessages(
        incomingMessages: List<FirestoreMessage>,
        isBlocked: suspend (senderId: String) -> Boolean,
        onMessagePurged: (suspend (senderId: String) -> Unit)? = null
    ): List<FirestoreMessage> {
        val result = mutableListOf<FirestoreMessage>()
        for (msg in incomingMessages) {
            if (isBlocked(msg.senderId)) {
                onMessagePurged?.invoke(msg.senderId)
            } else {
                result.add(msg)
            }
        }
        return result
    }
}
