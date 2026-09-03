package com.example.data.repository

import com.example.data.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class DesktopContactRepository : ContactRepository {

    private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(emptyMap())
    private val storageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg")
        if (!dir.exists()) dir.mkdirs()
        File(dir, "contacts.json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                if (content.isNotBlank()) {
                    val list = json.decodeFromString<List<ContactItem>>(content)
                    contactsFlow.value = list.associateBy { it.fingerprint }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun saveToDisk() {
        try {
            val list = contactsFlow.value.values.toList()
            val text = json.encodeToString(list)
            storageFile.writeText(text)
        } catch (_: Throwable) {}
    }

    override fun getContacts(): Flow<List<ContactItem>> {
        return contactsFlow.map { it.values.sortedByDescending { c -> c.addedAt } }
    }

    override suspend fun getContact(fingerprint: String): ContactItem? = withContext(Dispatchers.IO) {
        contactsFlow.value[fingerprint]
    }

    override suspend fun saveContact(contact: ContactItem) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        val toSave = if (contact.addedAt > 0) contact else contact.copy(addedAt = System.currentTimeMillis())
        current[contact.fingerprint] = toSave
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun setVerified(fingerprint: String, verified: Boolean) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        val c = current[fingerprint] ?: return@withContext
        current[fingerprint] = c.copy(verified = verified)
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun updateAuthUid(fingerprint: String, newUid: String) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        val c = current[fingerprint] ?: return@withContext
        current[fingerprint] = c.copy(currentAuthUid = newUid)
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun deleteContact(fingerprint: String) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        current.remove(fingerprint)
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun panicWipe(): Int = withContext(Dispatchers.IO) {
        val count = contactsFlow.value.size
        contactsFlow.value = emptyMap()
        try {
            if (storageFile.exists()) {
                // Anti-forensic zeroization before delete
                storageFile.writeBytes(ByteArray(storageFile.length().toInt().coerceAtLeast(1)))
                storageFile.delete()
            }
        } catch (_: Throwable) {}
        count
    }
}
