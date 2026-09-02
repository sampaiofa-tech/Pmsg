package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.EphemeralMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM ephemeral_messages WHERE roomId = :roomId AND expiresAt > :currentTime AND isShredded = 0 ORDER BY timestamp ASC")
    fun getActiveMessagesForRoom(roomId: String, currentTime: Long): Flow<List<EphemeralMessage>>

    @Query("SELECT * FROM ephemeral_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getAllMessagesForRoom(roomId: String): Flow<List<EphemeralMessage>>

    @Query("SELECT COUNT(*) FROM ephemeral_messages WHERE expiresAt > :currentTime AND isShredded = 0")
    fun getActiveMessageCount(currentTime: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: EphemeralMessage): Long

    @Query("UPDATE ephemeral_messages SET isDelivered = 1 WHERE id = :id")
    suspend fun markMessageDelivered(id: Long)

    @Query("UPDATE ephemeral_messages SET isRead = 1, readAt = :readAt WHERE id = :id")
    suspend fun markMessageRead(id: Long, readAt: Long)

    @Query("UPDATE ephemeral_messages SET isRead = 1, readAt = :readAt WHERE roomId = :roomId AND senderId != 'ME' AND isRead = 0")
    suspend fun markIncomingMessagesAsRead(roomId: String, readAt: Long)

    @Query("UPDATE ephemeral_messages SET isViewed = 1, viewedAt = :viewedAt WHERE id = :id")
    suspend fun markMessageViewed(id: Long, viewedAt: Long)

    @Query("UPDATE ephemeral_messages SET isShredded = 1, content = :noise, mediaUri = NULL, fileName = NULL WHERE id = :id")
    suspend fun overwriteMessageContent(id: Long, noise: String)

    @Query("UPDATE ephemeral_messages SET isShredded = 1, content = :noise, mediaUri = NULL, fileName = NULL WHERE roomId = :roomId")
    suspend fun overwriteRoomMessages(roomId: String, noise: String)

    @Query("UPDATE ephemeral_messages SET isShredded = 1, content = :noise, mediaUri = NULL, fileName = NULL")
    suspend fun overwriteAllMessages(noise: String)

    @Query("DELETE FROM ephemeral_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    /**
     * Purges and permanently erases all expired or shredded messages.
     * Leaves zero traces behind.
     */
    @Query("DELETE FROM ephemeral_messages WHERE expiresAt <= :currentTime OR isShredded = 1")
    suspend fun purgeExpiredMessages(currentTime: Long): Int

    /**
     * Purges all messages older than a given absolute cutoff timestamp (e.g., older than 24 hours).
     */
    @Query("DELETE FROM ephemeral_messages WHERE timestamp <= :cutoffTime")
    suspend fun deleteMessagesOlderThan(cutoffTime: Long): Int

    /**
     * Incinerates / burns an entire channel's messages with zero traces.
     */
    @Query("DELETE FROM ephemeral_messages WHERE roomId = :roomId")
    suspend fun incinerateRoomMessages(roomId: String): Int

    /**
     * PANIC WIPE: Instantly erases all messages across every channel.
     */
    @Query("DELETE FROM ephemeral_messages")
    suspend fun panicWipeAllMessages(): Int
}
