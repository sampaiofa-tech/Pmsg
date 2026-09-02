package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BurnerChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM burner_channels ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllChannels(): Flow<List<BurnerChannel>>

    @Query("SELECT * FROM burner_channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): BurnerChannel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: BurnerChannel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllDefaultChannels(channels: List<BurnerChannel>)

    @Query("UPDATE burner_channels SET lastMessagePreview = :preview, lastMessageTimestamp = :timestamp WHERE id = :channelId")
    suspend fun updateLastMessage(channelId: String, preview: String, timestamp: Long)

    @Query("DELETE FROM burner_channels WHERE lastMessageTimestamp <= :cutoffTime")
    suspend fun deleteChannelsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM burner_channels WHERE id = :id")
    suspend fun deleteChannelById(id: String)

    @Query("DELETE FROM burner_channels")
    suspend fun panicWipeAllChannels(): Int
}
