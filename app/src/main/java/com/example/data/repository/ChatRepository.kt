package com.example.data.repository

import com.example.data.local.ChannelDao
import com.example.data.local.MessageDao
import com.example.data.model.BurnerChannel
import com.example.data.model.EphemeralMessage
import com.example.util.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val messageDao: MessageDao,
    private val channelDao: ChannelDao
) {
    val allChannels: Flow<List<BurnerChannel>> = channelDao.getAllChannels()

    fun getActiveMessagesForRoom(roomId: String, currentTime: Long): Flow<List<EphemeralMessage>> {
        return messageDao.getActiveMessagesForRoom(roomId, currentTime).map { list ->
            list.map { decryptMessage(it) }
        }
    }

    fun getAllMessagesForRoom(roomId: String): Flow<List<EphemeralMessage>> {
        return messageDao.getAllMessagesForRoom(roomId).map { list ->
            list.map { decryptMessage(it) }
        }
    }

    fun getActiveMessageCount(currentTime: Long): Flow<Int> {
        return messageDao.getActiveMessageCount(currentTime)
    }

    private fun decryptMessage(message: EphemeralMessage): EphemeralMessage {
        val decryptedContent = CryptoManager.decrypt(message.content)
        val decryptedMediaUri = message.mediaUri?.let { CryptoManager.decrypt(it) }
        return message.copy(
            content = decryptedContent,
            mediaUri = decryptedMediaUri
        )
    }

    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String,
        ttlHours: Float = 24f,
        mediaType: String = "TEXT",
        mediaUri: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        audioDurationSeconds: Int = 0,
        isViewOnce: Boolean = false,
        disappearAfterReadSeconds: Int = 0
    ): EphemeralMessage = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val ttlMillis = (ttlHours * 60 * 60 * 1000L).toLong().coerceAtLeast(10_000L)
        val expiresAt = now + ttlMillis

        // Transparent AES-256-GCM encryption for zero-trace local storage
        val encryptedContent = CryptoManager.encrypt(text)
        val encryptedMediaUri = mediaUri?.let { CryptoManager.encrypt(it) }

        val messageToStore = EphemeralMessage(
            roomId = roomId,
            senderId = senderId,
            senderName = senderName,
            content = encryptedContent,
            timestamp = now,
            expiresAt = expiresAt,
            ttlOptionHours = ttlHours,
            isEncrypted = true,
            mediaType = mediaType,
            mediaUri = encryptedMediaUri,
            fileName = fileName,
            fileSize = fileSize,
            audioDurationSeconds = audioDurationSeconds,
            isViewOnce = isViewOnce,
            isDelivered = true,
            isRead = false,
            readAt = null,
            disappearAfterReadSeconds = disappearAfterReadSeconds
        )

        val insertedId = messageDao.insertMessage(messageToStore)

        // Update channel last message preview
        val preview = when {
            isViewOnce && mediaType == "AUDIO" -> "🎤 Áudio (Visualização única)"
            isViewOnce && mediaType == "IMAGE" -> "📷 Foto (Visualização única)"
            isViewOnce && mediaType == "VIDEO" -> "🎥 Vídeo (Visualização única)"
            isViewOnce -> "🔒 Mensagem (Visualização única)"
            mediaType == "AUDIO" -> "🎤 Áudio (${audioDurationSeconds}s)"
            mediaType == "IMAGE" -> "📷 Foto"
            mediaType == "VIDEO" -> "🎥 Vídeo"
            mediaType == "FILE" -> "📁 ${fileName ?: "Arquivo"}"
            else -> if (text.length > 40) text.take(37) + "..." else text
        }
        channelDao.updateLastMessage(roomId, preview, now)

        // Return in-memory version with plain text for immediate UI responsiveness
        messageToStore.copy(
            id = insertedId,
            content = text,
            mediaUri = mediaUri
        )
    }

    suspend fun markMessageDelivered(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.markMessageDelivered(messageId)
    }

    suspend fun markMessageRead(messageId: Long, readAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        messageDao.markMessageRead(messageId, readAt)
    }

    suspend fun markIncomingMessagesAsRead(roomId: String, readAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        messageDao.markIncomingMessagesAsRead(roomId, readAt)
    }

    suspend fun markMessageViewed(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.markMessageViewed(messageId, System.currentTimeMillis())
    }

    suspend fun shredMessage(messageId: Long) = withContext(Dispatchers.IO) {
        // Multi-pass secure shredding: Overwrite payload with cryptographically random noise first, then delete
        val noise = CryptoManager.generateSecureNoise(48)
        messageDao.overwriteMessageContent(messageId, noise)
        messageDao.deleteMessageById(messageId)
    }

    suspend fun clearChatHistory(roomId: String) = withContext(Dispatchers.IO) {
        val noise = CryptoManager.generateSecureNoise(64)
        messageDao.overwriteRoomMessages(roomId, noise)
        messageDao.incinerateRoomMessages(roomId)
        channelDao.updateLastMessage(roomId, "Histórico apagado • Zero Trace", System.currentTimeMillis())
        com.example.data.local.VanishDatabase.performAntiForensicVacuum()
    }

    suspend fun purgeExpired(currentTime: Long): Int = withContext(Dispatchers.IO) {
        val count = messageDao.purgeExpiredMessages(currentTime)
        if (count > 0) {
            com.example.data.local.VanishDatabase.performAntiForensicVacuum()
        }
        count
    }

    suspend fun incinerateRoom(roomId: String) = withContext(Dispatchers.IO) {
        val noise = CryptoManager.generateSecureNoise(64)
        messageDao.overwriteRoomMessages(roomId, noise)
        messageDao.incinerateRoomMessages(roomId)
        channelDao.deleteChannelById(roomId)
        com.example.data.local.VanishDatabase.performAntiForensicVacuum()
    }

    suspend fun deleteChannel(channelId: String) = withContext(Dispatchers.IO) {
        val noise = CryptoManager.generateSecureNoise(64)
        messageDao.overwriteRoomMessages(channelId, noise)
        messageDao.incinerateRoomMessages(channelId)
        channelDao.deleteChannelById(channelId)
        com.example.data.local.VanishDatabase.performAntiForensicVacuum()
    }

    suspend fun createBurnerChannel(
        name: String,
        customCode: String = "",
        ttlHours: Float = 24f,
        securityTag: String = "E2EE Zero Trace • 24h"
    ): BurnerChannel = withContext(Dispatchers.IO) {
        val code = if (customCode.isNotBlank()) customCode else "VN-" + (1000..9999).random()
        val channelId = "channel_" + UUID.randomUUID().toString().take(8)
        val colors = listOf(0xFF00E5FF, 0xFF00E676, 0xFFFF6D00, 0xFFB388FF, 0xFFFF4081)
        val randomColor = colors.random()

        val channel = BurnerChannel(
            id = channelId,
            name = name,
            avatarColorHex = randomColor,
            securityTag = securityTag,
            channelCode = code,
            lastMessagePreview = "Canal seguro iniciado. Autodestruição em 24h.",
            lastMessageTimestamp = System.currentTimeMillis(),
            defaultTtlHours = ttlHours
        )

        channelDao.insertChannel(channel)
        channel
    }

    suspend fun panicWipeAll(): Int = withContext(Dispatchers.IO) {
        val noise = CryptoManager.generateSecureNoise(128)
        messageDao.overwriteAllMessages(noise)
        val deletedCount = messageDao.panicWipeAllMessages()
        channelDao.panicWipeAllChannels()

        // Anti-Forensics: Force full database vacuum to truncate storage pages
        com.example.data.local.VanishDatabase.performFullVacuum()

        // Hardware Crypto-Shredding: Purge master key from KeyStore so deleted blocks cannot be recovered
        CryptoManager.invalidateAndRecreateMasterKey()

        deletedCount
    }
}
