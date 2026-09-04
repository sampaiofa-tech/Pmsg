package com.example.data.repository

import com.example.data.model.BlockedContact
import com.example.data.model.ContactItem
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContacts(): Flow<List<ContactItem>>
    suspend fun getContact(fingerprint: String): ContactItem?
    suspend fun saveContact(contact: ContactItem)
    suspend fun setVerified(fingerprint: String, verified: Boolean)
    suspend fun updateAuthUid(fingerprint: String, newUid: String)
    suspend fun deleteContact(fingerprint: String)
    suspend fun panicWipe(): Int

    // Client-side blocklist (zero-knowledge server-side)
    fun getBlockedContacts(): Flow<List<BlockedContact>>
    suspend fun blockContact(fingerprint: String)
    suspend fun unblockContact(fingerprint: String)
    suspend fun isContactBlocked(fingerprint: String): Boolean
    suspend fun recordBlockedPurge(fingerprint: String)
    fun getBlockedPurgeCount(): Flow<Int>
}

expect object ContactRepositoryProvider {
    fun get(): ContactRepository
}

