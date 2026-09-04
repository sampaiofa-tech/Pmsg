package com.example

import com.example.data.model.BlockedContact
import com.example.data.model.ContactItem
import com.example.data.model.FirestoreMessage
import com.example.data.network.FirestoreMessageSync
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContactBlocklistTest {

    private fun createTestRepository(): ContactRepository {
        return object : ContactRepository {
            private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(emptyMap())
            private val blockedFlow = MutableStateFlow<Map<String, BlockedContact>>(emptyMap())
            private val purgeCountFlow = MutableStateFlow(0)

            override fun getContacts(): Flow<List<ContactItem>> =
                contactsFlow.map { it.values.toList() }

            override suspend fun getContact(fingerprint: String): ContactItem? =
                contactsFlow.value[fingerprint]

            override suspend fun saveContact(contact: ContactItem) {
                val current = contactsFlow.value.toMutableMap()
                current[contact.fingerprint] = contact
                contactsFlow.value = current
            }

            override suspend fun setVerified(fingerprint: String, verified: Boolean) {}
            override suspend fun updateAuthUid(fingerprint: String, newUid: String) {}
            override suspend fun deleteContact(fingerprint: String) {
                val current = contactsFlow.value.toMutableMap()
                current.remove(fingerprint)
                contactsFlow.value = current
            }

            override suspend fun panicWipe(): Int {
                val c = contactsFlow.value.size + blockedFlow.value.size
                contactsFlow.value = emptyMap()
                blockedFlow.value = emptyMap()
                purgeCountFlow.value = 0
                return c
            }

            override fun getBlockedContacts(): Flow<List<BlockedContact>> =
                blockedFlow.map { it.values.sortedByDescending { b -> b.blockedAt } }

            override suspend fun blockContact(fingerprint: String) {
                val current = blockedFlow.value.toMutableMap()
                current[fingerprint] = BlockedContact(fingerprint, 1000L)
                blockedFlow.value = current
            }

            override suspend fun unblockContact(fingerprint: String) {
                val current = blockedFlow.value.toMutableMap()
                current.remove(fingerprint)
                blockedFlow.value = current
            }

            override suspend fun isContactBlocked(fingerprint: String): Boolean =
                blockedFlow.value.containsKey(fingerprint)

            override suspend fun recordBlockedPurge(fingerprint: String) {
                purgeCountFlow.value += 1
            }

            override fun getBlockedPurgeCount(): Flow<Int> = purgeCountFlow
        }
    }

    @Test
    fun testBlockContactAndVerifyIsBlocked() = runTest {
        val repo = createTestRepository()
        val fp = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

        assertFalse(repo.isContactBlocked(fp), "Contact must not be blocked initially")
        assertEquals(0, repo.getBlockedContacts().first().size)

        repo.blockContact(fp)

        assertTrue(repo.isContactBlocked(fp), "Contact must be blocked after blockContact")
        val blockedList = repo.getBlockedContacts().first()
        assertEquals(1, blockedList.size)
        assertEquals(fp, blockedList[0].fingerprint)
    }

    @Test
    fun testUnblockContactAndVerifyUnblocked() = runTest {
        val repo = createTestRepository()
        val fp = "abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd"

        repo.blockContact(fp)
        assertTrue(repo.isContactBlocked(fp))

        repo.unblockContact(fp)
        assertFalse(repo.isContactBlocked(fp), "Contact must be unblocked after unblockContact")
        assertEquals(0, repo.getBlockedContacts().first().size)
    }

    @Test
    fun testIncomingMessageFromBlockedContactIsDiscardedAndCounted() = runTest {
        val repo = createTestRepository()
        val blockedFp = "bad_actor_fingerprint_hex_64_chars_length_sample_000000000000000000"
        val benignFp = "alice_benign_fingerprint_hex_64_chars_length_sample_11111111111111"

        repo.blockContact(blockedFp)

        val incoming = listOf(
            FirestoreMessage(
                id = "msg_1",
                ciphertext = "valid_ciphertext",
                iv = "iv_1",
                senderId = benignFp,
                recipientId = "me",
                expiresAt = 999999999L
            ),
            FirestoreMessage(
                id = "msg_2_spam",
                ciphertext = "spam_ciphertext",
                iv = "iv_2",
                senderId = blockedFp,
                recipientId = "me",
                expiresAt = 999999999L
            )
        )

        // Enforce blocklist on fetch
        val filtered = FirestoreMessageSync.filterBlockedMessages(
            incomingMessages = incoming,
            isBlocked = { senderId -> repo.isContactBlocked(senderId) },
            onMessagePurged = { senderId -> repo.recordBlockedPurge(senderId) }
        )

        assertEquals(1, filtered.size, "Only messages from non-blocked senders must survive filtering")
        assertEquals("msg_1", filtered[0].id)
        assertEquals(benignFp, filtered[0].senderId)

        // Verify purge count incremented
        assertEquals(1, repo.getBlockedPurgeCount().first())
    }

    @Test
    fun testUnblockRestoresMessageAcceptance() = runTest {
        val repo = createTestRepository()
        val fp = "target_fingerprint_to_test_unblock_cycle_0000000000000000000000000"

        // 1. Block -> Discarded
        repo.blockContact(fp)
        val msg = FirestoreMessage(
            id = "msg_unblock_test",
            ciphertext = "c",
            iv = "i",
            senderId = fp,
            recipientId = "me",
            expiresAt = 1000L
        )
        val filteredWhileBlocked = FirestoreMessageSync.filterBlockedMessages(
            incomingMessages = listOf(msg),
            isBlocked = { repo.isContactBlocked(it) }
        )
        assertTrue(filteredWhileBlocked.isEmpty(), "Message must be discarded while contact is blocked")

        // 2. Unblock -> Accepted
        repo.unblockContact(fp)
        val filteredAfterUnblock = FirestoreMessageSync.filterBlockedMessages(
            incomingMessages = listOf(msg),
            isBlocked = { repo.isContactBlocked(it) }
        )
        assertEquals(1, filteredAfterUnblock.size, "Message must be accepted after contact is unblocked")
        assertEquals("msg_unblock_test", filteredAfterUnblock[0].id)
    }

    @Test
    fun testPanicWipeClearsBlockedContactsAndPurgeCount() = runTest {
        val repo = createTestRepository()
        repo.blockContact("fp_1")
        repo.blockContact("fp_2")
        repo.recordBlockedPurge("fp_1")

        assertEquals(2, repo.getBlockedContacts().first().size)
        assertEquals(1, repo.getBlockedPurgeCount().first())

        repo.panicWipe()

        assertEquals(0, repo.getBlockedContacts().first().size)
        assertEquals(0, repo.getBlockedPurgeCount().first())
        assertFalse(repo.isContactBlocked("fp_1"))
    }
}
