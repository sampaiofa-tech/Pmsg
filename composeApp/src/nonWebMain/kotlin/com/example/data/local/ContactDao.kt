package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY addedAt DESC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getContactByFingerprint(fingerprint: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Query("UPDATE contacts SET verified = :verified WHERE fingerprint = :fingerprint")
    suspend fun updateVerified(fingerprint: String, verified: Boolean)

    @Query("UPDATE contacts SET currentAuthUid = :newUid WHERE fingerprint = :fingerprint")
    suspend fun updateAuthUid(fingerprint: String, newUid: String)

    @Query("DELETE FROM contacts WHERE fingerprint = :fingerprint")
    suspend fun deleteContact(fingerprint: String)

    @Query("DELETE FROM contacts")
    suspend fun panicWipeAllContacts(): Int
}
