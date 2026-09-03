package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VanishDatabase
import com.example.data.model.ContactItem
import com.example.data.repository.AndroidContactRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactRepositoryTest {

    private lateinit var db: VanishDatabase
    private lateinit var repository: AndroidContactRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VanishDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AndroidContactRepository(db.contactDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveContact_withZeroPlaintextOnDisk() = runTest {
        val contact = ContactItem(
            fingerprint = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            pubKey = "dGVzdC1wdWJsaWMta2V5LTMyeHh4eHh4eHh4eHh4eHg=",
            currentAuthUid = "uid_bob_123",
            displayName = "Bob Confidential",
            securityNumber = "12345 67890 12345 67890 12345 67890 12345 67890 12345 67890 12345 67890",
            verified = false
        )

        repository.saveContact(contact)

        // 1. Verify on-disk entity: displayName MUST BE ENCRYPTED, NOT PLAINTEXT
        val rawEntity = db.contactDao().getContactByFingerprint(contact.fingerprint)
        assertNotNull(rawEntity)
        assertTrue(
            "Raw database displayName must be encrypted, got: ${rawEntity!!.displayNameEncrypted}",
            rawEntity.displayNameEncrypted.startsWith("ENC")
        )
        assertFalse(
            "Raw database must NEVER contain plaintext contact name!",
            rawEntity.displayNameEncrypted.contains("Bob Confidential")
        )

        // 2. Verify repository transparent decryption
        val retrieved = repository.getContact(contact.fingerprint)
        assertNotNull(retrieved)
        assertEquals("Bob Confidential", retrieved!!.displayName)
        assertEquals(contact.fingerprint, retrieved.fingerprint)
        assertEquals(contact.pubKey, retrieved.pubKey)
        assertEquals(contact.currentAuthUid, retrieved.currentAuthUid)
        assertFalse(retrieved.verified)

        // 3. Verify in flow
        val all = repository.getContacts().first()
        assertEquals(1, all.size)
        assertEquals("Bob Confidential", all[0].displayName)
    }

    @Test
    fun testUpdateVerificationAndAuthUid() = runTest {
        val fp = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        val contact = ContactItem(
            fingerprint = fp,
            pubKey = "pk_alice",
            currentAuthUid = "uid_alice_old",
            displayName = "Alice",
            securityNumber = "00000 11111 22222 33333 44444 55555 66666 77777 88888 99999 00000 11111",
            verified = false
        )

        repository.saveContact(contact)

        // Update verification
        repository.setVerified(fp, true)
        val afterVerify = repository.getContact(fp)
        assertNotNull(afterVerify)
        assertTrue(afterVerify!!.verified)

        // Update Auth UID
        repository.updateAuthUid(fp, "uid_alice_new")
        val afterUid = repository.getContact(fp)
        assertNotNull(afterUid)
        assertEquals("uid_alice_new", afterUid!!.currentAuthUid)
    }

    @Test
    fun testDeleteAndPanicWipe() = runTest {
        val contact1 = ContactItem(
            fingerprint = "1111111111111111111111111111111111111111111111111111111111111111",
            pubKey = "pk_1",
            currentAuthUid = "uid_1",
            displayName = "Contact 1",
            securityNumber = "11111",
            verified = false
        )
        val contact2 = ContactItem(
            fingerprint = "2222222222222222222222222222222222222222222222222222222222222222",
            pubKey = "pk_2",
            currentAuthUid = "uid_2",
            displayName = "Contact 2",
            securityNumber = "22222",
            verified = true
        )

        repository.saveContact(contact1)
        repository.saveContact(contact2)

        assertEquals(2, repository.getContacts().first().size)

        // Delete single contact
        repository.deleteContact(contact1.fingerprint)
        assertNull(repository.getContact(contact1.fingerprint))
        assertEquals(1, repository.getContacts().first().size)

        // Panic wipe
        val wiped = repository.panicWipe()
        assertTrue(wiped >= 1)
        assertEquals(0, repository.getContacts().first().size)
    }
}
