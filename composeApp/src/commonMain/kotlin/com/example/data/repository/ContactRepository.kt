package com.example.data.repository

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
}

expect object ContactRepositoryProvider {
    fun get(): ContactRepository
}
