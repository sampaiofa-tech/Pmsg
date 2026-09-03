package com.example.security

import com.example.security.identity.IdentityCryptoManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IdentityCryptoTest {

    @Test
    fun testDeterministicDerivationFromEntropy() {
        val fixedEntropy = ByteArray(16) { it.toByte() }
        val id1 = IdentityCryptoManager.generateNewIdentity(fixedEntropy)
        val id2 = IdentityCryptoManager.generateNewIdentity(fixedEntropy)

        assertEquals(id1.mnemonic, id2.mnemonic)
        assertTrue(id1.keyPair.privateKey.contentEquals(id2.keyPair.privateKey))
        assertTrue(id1.keyPair.publicKey.contentEquals(id2.keyPair.publicKey))
        assertEquals(id1.keyPair.fingerprintHex, id2.keyPair.fingerprintHex)
        assertEquals(id1.keyPair.safetyNumber, id2.keyPair.safetyNumber)
    }

    @Test
    fun testRestorationFromMnemonic() {
        val originalIdentity = IdentityCryptoManager.generateNewIdentity()
        val restoreResult = IdentityCryptoManager.restoreFromMnemonic(originalIdentity.mnemonic)

        assertTrue(restoreResult.isSuccess)
        val restoredKeyPair = restoreResult.getOrThrow()

        assertTrue(originalIdentity.keyPair.privateKey.contentEquals(restoredKeyPair.privateKey))
        assertTrue(originalIdentity.keyPair.publicKey.contentEquals(restoredKeyPair.publicKey))
        assertEquals(originalIdentity.keyPair.fingerprintHex, restoredKeyPair.fingerprintHex)
        assertEquals(originalIdentity.keyPair.safetyNumber, restoredKeyPair.safetyNumber)
    }

    @Test
    fun testSafetyNumberFormat60Digits() {
        val id = IdentityCryptoManager.generateNewIdentity()
        val safetyNumber = id.keyPair.safetyNumber

        // Should be 12 blocks of 5 digits separated by spaces: total length 60 + 11 spaces = 71 chars
        assertEquals(71, safetyNumber.length)
        val blocks = safetyNumber.split(" ")
        assertEquals(12, blocks.size)
        for (b in blocks) {
            assertEquals(5, b.length)
            assertTrue(b.all { it.isDigit() })
        }
    }

    @Test
    fun testPairSafetyNumberSymmetry() {
        val alice = IdentityCryptoManager.generateNewIdentity()
        val bob = IdentityCryptoManager.generateNewIdentity()

        val safetyNumberAliceBob = IdentityCryptoManager.computePairSafetyNumber(alice.keyPair.publicKey, bob.keyPair.publicKey)
        val safetyNumberBobAlice = IdentityCryptoManager.computePairSafetyNumber(bob.keyPair.publicKey, alice.keyPair.publicKey)

        assertEquals(safetyNumberAliceBob, safetyNumberBobAlice)
        assertEquals(71, safetyNumberAliceBob.length)
        val blocks = safetyNumberBobAlice.split(" ")
        assertEquals(12, blocks.size)
        for (b in blocks) {
            assertEquals(5, b.length)
            assertTrue(b.all { it.isDigit() })
        }
    }
}
