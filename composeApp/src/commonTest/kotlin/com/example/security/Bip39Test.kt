package com.example.security

import com.example.security.identity.Bip39Portuguese
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class Bip39Test {

    @Test
    fun testKnownVectorDeterminism() {
        // 16 zero bytes
        val zeroEntropy = ByteArray(16)
        val words = Bip39Portuguese.entropyToMnemonic(zeroEntropy)
        assertEquals(12, words.size)
        // First word for 0 is "abacate"
        assertEquals("abacate", words[0])

        val recoveredResult = Bip39Portuguese.mnemonicToEntropy(words)
        assertTrue(recoveredResult.isSuccess)
        val recovered = recoveredResult.getOrThrow()
        assertTrue(zeroEntropy.contentEquals(recovered))
    }

    @Test
    fun testMultipleRandomRoundtrips() {
        val random = Random(42)
        for (i in 0 until 50) {
            val entropy = ByteArray(16)
            random.nextBytes(entropy)

            val mnemonic = Bip39Portuguese.entropyToMnemonic(entropy)
            assertEquals(12, mnemonic.size)

            val recoveredResult = Bip39Portuguese.mnemonicToEntropy(mnemonic)
            assertTrue(recoveredResult.isSuccess, "Failed on iteration $i")
            val recovered = recoveredResult.getOrThrow()
            assertTrue(entropy.contentEquals(recovered), "Entropy mismatch on iteration $i")
        }
    }

    @Test
    fun testInvalidWordCountRejected() {
        val elevenWords = List(11) { "abacate" }
        val result = Bip39Portuguese.mnemonicToEntropy(elevenWords)
        assertTrue(result.isFailure)

        val thirteenWords = List(13) { "abacate" }
        val result13 = Bip39Portuguese.mnemonicToEntropy(thirteenWords)
        assertTrue(result13.isFailure)
    }

    @Test
    fun testInvalidWordRejected() {
        val words = mutableListOf(
            "abacate", "abaixo", "abalar", "abater",
            "abduzir", "abelha", "aberto", "abismo",
            "abotoar", "abranger", "abreviar", "palavranaoinclusa"
        )
        val result = Bip39Portuguese.mnemonicToEntropy(words)
        assertTrue(result.isFailure)
    }

    @Test
    fun testInvalidChecksumRejected() {
        val entropy = ByteArray(16) { 0x55.toByte() }
        val validMnemonic = Bip39Portuguese.entropyToMnemonic(entropy).toMutableList()

        // Mutate the last word to another valid word, which will corrupt the 4-bit checksum
        val originalLastWord = validMnemonic[11]
        val otherWord = if (originalLastWord == "abacate") "abaixo" else "abacate"
        validMnemonic[11] = otherWord

        val result = Bip39Portuguese.mnemonicToEntropy(validMnemonic)
        assertTrue(result.isFailure, "Corrupted checksum must be rejected")
    }

    @Test
    fun testCaseAndWhitespaceInsensitive() {
        val entropy = ByteArray(16) { 0xAA.toByte() }
        val mnemonic = Bip39Portuguese.entropyToMnemonic(entropy)
        val transformed = mnemonic.map { "  " + it.uppercase() + " " }

        val recoveredResult = Bip39Portuguese.mnemonicToEntropy(transformed)
        assertTrue(recoveredResult.isSuccess)
        assertTrue(entropy.contentEquals(recoveredResult.getOrThrow()))
    }
}
