package com.example.data.repository

import com.example.data.local.VanishDatabase
import com.example.data.model.BlockedContact
import com.example.data.model.ContactItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

actual object ContactRepositoryProvider {
    private var testRepository: ContactRepository? = null

    fun setTestRepository(repo: ContactRepository?) {
        testRepository = repo
    }

    actual fun get(): ContactRepository {
        testRepository?.let { return it }

        val db = VanishDatabase.getInstance()
        if (db != null) {
            return AndroidContactRepository(db.contactDao(), db.blockedContactDao())
        }

        // Fallback in-memory instance if database hasn't been initialized yet
        return InMemoryContactRepository
    }
}

private object InMemoryContactRepository : ContactRepository {
    private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(emptyMap())
    private val blockedFlow = MutableStateFlow<Map<String, BlockedContact>>(emptyMap())
    private val purgeCountFlow = MutableStateFlow(0)

    override fun getContacts(): Flow<List<ContactItem>> {
        return contactsFlow.map { it.values.sortedByDescending { c -> c.addedAt } }
    }

    override suspend fun getContact(fingerprint: String): ContactItem? {
        return contactsFlow.value[fingerprint]
    }

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
        blockedFlow.value = emptyMap()
        purgeCountFlow.value = 0
        return count
    }

    override fun getBlockedContacts(): Flow<List<BlockedContact>> {
        return blockedFlow.map { it.values.sortedByDescending { b -> b.blockedAt } }
    }

    override suspend fun blockContact(fingerprint: String) {
        val current = blockedFlow.value.toMutableMap()
        current[fingerprint] = BlockedContact(fingerprint, System.currentTimeMillis())
        blockedFlow.value = current
    }

    override suspend fun unblockContact(fingerprint: String) {
        val current = blockedFlow.value.toMutableMap()
        current.remove(fingerprint)
        blockedFlow.value = current
    }

    override suspend fun isContactBlocked(fingerprint: String): Boolean {
        return blockedFlow.value.containsKey(fingerprint)
    }

    override suspend fun recordBlockedPurge(fingerprint: String) {
        purgeCountFlow.value += 1
    }

    override fun getBlockedPurgeCount(): Flow<Int> = purgeCountFlow
}
