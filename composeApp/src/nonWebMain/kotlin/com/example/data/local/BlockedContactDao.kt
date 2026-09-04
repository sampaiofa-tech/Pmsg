package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockedContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedContactDao {

    @Query("SELECT * FROM blocked_contacts ORDER BY blockedAt DESC")
    fun getAllBlockedContacts(): Flow<List<BlockedContactEntity>>

    @Query("SELECT * FROM blocked_contacts WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getBlockedContact(fingerprint: String): BlockedContactEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_contacts WHERE fingerprint = :fingerprint)")
    suspend fun isBlocked(fingerprint: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedContact(contact: BlockedContactEntity)

    @Query("DELETE FROM blocked_contacts WHERE fingerprint = :fingerprint")
    suspend fun deleteBlockedContact(fingerprint: String)

    @Query("DELETE FROM blocked_contacts")
    suspend fun panicWipeAllBlockedContacts(): Int
}
