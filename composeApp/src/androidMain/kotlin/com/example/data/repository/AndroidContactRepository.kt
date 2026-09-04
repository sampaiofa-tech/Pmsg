package com.example.data.repository

import com.example.data.local.BlockedContactDao
import com.example.data.local.ContactDao
import com.example.data.model.BlockedContact
import com.example.data.model.BlockedContactEntity
import com.example.data.model.Contact
import com.example.data.model.ContactItem
import com.example.util.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidContactRepository(
    private val contactDao: ContactDao,
    private val blockedContactDao: BlockedContactDao? = null
) : ContactRepository {

    private val blockedPurgeCountFlow = MutableStateFlow(0)

    override fun getContacts(): Flow<List<ContactItem>> {
        return contactDao.getAllContacts().map { list ->
            list.map { contact ->
                val decryptedName = try {
                    CryptoManager.decrypt(contact.displayNameEncrypted)
                } catch (_: Throwable) {
                    "Contato Desconhecido"
                }
                contact.toItem(decryptedName)
            }
        }
    }

    override suspend fun getContact(fingerprint: String): ContactItem? = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactByFingerprint(fingerprint) ?: return@withContext null
        val decryptedName = try {
            CryptoManager.decrypt(contact.displayNameEncrypted)
        } catch (_: Throwable) {
            "Contato Desconhecido"
        }
        contact.toItem(decryptedName)
    }

    override suspend fun saveContact(contact: ContactItem) = withContext(Dispatchers.IO) {
        val encryptedName = CryptoManager.encrypt(contact.displayName)
        val entity = Contact(
            fingerprint = contact.fingerprint,
            pubKey = contact.pubKey,
            currentAuthUid = contact.currentAuthUid,
            displayNameEncrypted = encryptedName,
            securityNumber = contact.securityNumber,
            verified = contact.verified,
            addedAt = if (contact.addedAt > 0) contact.addedAt else System.currentTimeMillis()
        )
        contactDao.insertContact(entity)
    }

    override suspend fun setVerified(fingerprint: String, verified: Boolean) = withContext(Dispatchers.IO) {
        contactDao.updateVerified(fingerprint, verified)
    }

    override suspend fun updateAuthUid(fingerprint: String, newUid: String) = withContext(Dispatchers.IO) {
        contactDao.updateAuthUid(fingerprint, newUid)
    }

    override suspend fun deleteContact(fingerprint: String) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(fingerprint)
    }

    override suspend fun panicWipe(): Int = withContext(Dispatchers.IO) {
        val count = contactDao.panicWipeAllContacts()
        blockedContactDao?.panicWipeAllBlockedContacts()
        blockedPurgeCountFlow.value = 0
        count
    }

    override fun getBlockedContacts(): Flow<List<BlockedContact>> {
        return blockedContactDao?.getAllBlockedContacts()?.map { list ->
            list.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    override suspend fun blockContact(fingerprint: String) = withContext(Dispatchers.IO) {
        blockedContactDao?.insertBlockedContact(
            BlockedContactEntity(
                fingerprint = fingerprint,
                blockedAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    override suspend fun unblockContact(fingerprint: String) = withContext(Dispatchers.IO) {
        blockedContactDao?.deleteBlockedContact(fingerprint)
        Unit
    }

    override suspend fun isContactBlocked(fingerprint: String): Boolean = withContext(Dispatchers.IO) {
        blockedContactDao?.isBlocked(fingerprint) ?: false
    }

    override suspend fun recordBlockedPurge(fingerprint: String) {
        blockedPurgeCountFlow.value += 1
    }

    override fun getBlockedPurgeCount(): Flow<Int> = blockedPurgeCountFlow
}
