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

    private val defaultSeed = listOf(
        ContactItem(
            fingerprint = "d6a7b83a2be2d050105bb3a34edc428977925e9d7b9222f78a9c2e3b238776d4",
            pubKey = "UWECg4c68X5+JbA4sLRCsCH3of2/Q4LNiu6ErnP5zyw=",
            currentAuthUid = "uid_alice_77",
            displayName = "Alice (Engenharia)",
            securityNumber = "34981 83294 02934 98123 48102 39182 49102 39182 49102 39182 49102 39182",
            verified = true,
            addedAt = 1725391000000L
        ),
        ContactItem(
            fingerprint = "e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7",
            pubKey = "YW5vdGhlci1wdWJsaWMta2V5LTMyeHh4eHh4eHh4eHh4",
            currentAuthUid = "uid_bob_88",
            displayName = "Bob (Diretoria)",
            securityNumber = "98123 48102 39182 34981 83294 02934 39182 49102 39182 49102 39182 49102",
            verified = false,
            addedAt = 1725390000000L
        )
    )

    private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(defaultSeed.associateBy { it.fingerprint })
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
                    return
                }
            }
            // Seed initial contacts for desktop experience
            val initial = listOf(
                ContactItem(
                    fingerprint = "d6a7b83a2be2d050105bb3a34edc428977925e9d7b9222f78a9c2e3b238776d4",
                    pubKey = "UWECg4c68X5+JbA4sLRCsCH3of2/Q4LNiu6ErnP5zyw=",
                    currentAuthUid = "uid_alice_77",
                    displayName = "Alice (Engenharia)",
                    securityNumber = "34981 83294 02934 98123 48102 39182 49102 39182 49102 39182 49102 39182",
                    verified = true,
                    addedAt = System.currentTimeMillis()
                ),
                ContactItem(
                    fingerprint = "e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7",
                    pubKey = "YW5vdGhlci1wdWJsaWMta2V5LTMyeHh4eHh4eHh4eHh4",
                    currentAuthUid = "uid_bob_88",
                    displayName = "Bob (Diretoria)",
                    securityNumber = "98123 48102 39182 34981 83294 02934 39182 49102 39182 49102 39182 49102",
                    verified = false,
                    addedAt = System.currentTimeMillis() - 3600_000L
                )
            )
            contactsFlow.value = initial.associateBy { it.fingerprint }
            saveToDisk()
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
        if (contactsFlow.value.isEmpty()) {
            loadFromDisk()
        }
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
