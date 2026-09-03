package com.example.data.repository

import com.example.data.local.ContactDao
import com.example.data.model.Contact
import com.example.data.model.ContactItem
import com.example.util.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidContactRepository(
    private val contactDao: ContactDao
) : ContactRepository {

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
        contactDao.panicWipeAllContacts()
    }
}
