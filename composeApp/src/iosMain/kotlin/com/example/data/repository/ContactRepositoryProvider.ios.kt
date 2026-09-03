package com.example.data.repository

import com.example.data.model.ContactItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

actual object ContactRepositoryProvider {
    private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(emptyMap())

    private val repository = object : ContactRepository {
        override fun getContacts(): Flow<List<ContactItem>> =
            contactsFlow.map { it.values.sortedByDescending { c -> c.addedAt } }

        override suspend fun getContact(fingerprint: String): ContactItem? =
            contactsFlow.value[fingerprint]

        override suspend fun saveContact(contact: ContactItem) {
            val current = contactsFlow.value.toMutableMap()
            current[contact.fingerprint] = contact
            contactsFlow.value = current
        }

        override suspend fun setVerified(fingerprint: String, verified: Boolean) {
            val current = contactsFlow.value.toMutableMap()
            val c = current[fingerprint] ?: return
            current[fingerprint] = c.copy(verified = verified)
            contactsFlow.value = current
        }

        override suspend fun updateAuthUid(fingerprint: String, newUid: String) {
            val current = contactsFlow.value.toMutableMap()
            val c = current[fingerprint] ?: return
            current[fingerprint] = c.copy(currentAuthUid = newUid)
            contactsFlow.value = current
        }

        override suspend fun deleteContact(fingerprint: String) {
            val current = contactsFlow.value.toMutableMap()
            current.remove(fingerprint)
            contactsFlow.value = current
        }

        override suspend fun panicWipe(): Int {
            val count = contactsFlow.value.size
            contactsFlow.value = emptyMap()
            return count
        }
    }

    actual fun get(): ContactRepository = repository
}
