package com.example.security.identity

import com.example.security.KeyVault
import kotlin.random.Random

data class IdentityKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray,
    val fingerprintHex: String,
    val safetyNumber: String, // 60 decimal digits (12 blocks of 5)
    val signingPrivateKey: ByteArray = ByteArray(0),
    val signingPublicKey: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IdentityKeyPair) return false
        return privateKey.contentEquals(other.privateKey) &&
                publicKey.contentEquals(other.publicKey) &&
                fingerprintHex == other.fingerprintHex &&
                safetyNumber == other.safetyNumber &&
                signingPrivateKey.contentEquals(other.signingPrivateKey) &&
                signingPublicKey.contentEquals(other.signingPublicKey)
    }

    override fun hashCode(): Int {
        var result = privateKey.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + fingerprintHex.hashCode()
        result = 31 * result + safetyNumber.hashCode()
        result = 31 * result + signingPrivateKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        return result
    }
}

data class ProvisionedIdentity(
    val mnemonic: List<String>,
    val keyPair: IdentityKeyPair,
    val entropy: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProvisionedIdentity) return false
        return mnemonic == other.mnemonic &&
                keyPair == other.keyPair &&
                entropy.contentEquals(other.entropy)
    }

    override fun hashCode(): Int {
        var result = mnemonic.hashCode()
        result = 31 * result + keyPair.hashCode()
        result = 31 * result + entropy.contentHashCode()
        return result
    }
}

/**
 * Core cryptographic engine for Pmsg Identity (v1.1).
 *
 * Implements:
 * 1. Deterministic BIP-39 PT-BR 128-bit entropy -> 12-word mnemonic.
 * 2. Argon2id key derivation -> 256-bit X25519 keypair (salt: "pmsg-v1-identity-seed").
 * 3. Deterministic Ed25519 signing keypair derivation (salt: "pmsg-v1-identity-signing").
 * 4. Fingerprint computation: SHA-256(pubKey) as 12 blocks of 5 digits (60 digits total).
 * 5. Pair Safety Number computation: SHA-256(min(pkA, pkB) + max(pkA, pkB)) as 60 digits.
 * 6. Proof-of-possession signing for technical identity routing updates.
 * 7. Envelope encryption of private keys and seed using KeyVault hardware keys.
 */
object IdentityCryptoManager {

    private val SALT = "pmsg-v1-identity-seed".encodeToByteArray()
    private val SIGNING_SALT = "pmsg-v1-identity-signing".encodeToByteArray()

    fun generateNewIdentity(providedEntropy: ByteArray? = null): ProvisionedIdentity {
        val entropy = providedEntropy ?: ByteArray(16).also { Random.nextBytes(it) }
        require(entropy.size == 16) { "Entropy must be exactly 16 bytes (128 bits)" }

        val mnemonic = Bip39Portuguese.entropyToMnemonic(entropy)
        val keyPair = deriveKeyPair(mnemonic)
        return ProvisionedIdentity(
            mnemonic = mnemonic,
            keyPair = keyPair,
            entropy = entropy
        )
    }

    fun deriveKeyPair(mnemonic: List<String>): IdentityKeyPair {
        val entropy = Bip39Portuguese.mnemonicToEntropy(mnemonic).getOrThrow()
        val seed = Sha256Digest.digest(entropy)

        // 1. X25519 Encryption KeyPair
        val rawPriv = Argon2Kmp.deriveKey(seed = seed, salt = SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)

        // RFC 7748 Clamping
        val clampedPriv = rawPriv.copyOf(32)
        clampedPriv[0] = (clampedPriv[0].toInt() and 248).toByte()
        clampedPriv[31] = (clampedPriv[31].toInt() and 127).toByte()
        clampedPriv[31] = (clampedPriv[31].toInt() or 64).toByte()

        val pubKey = IdentityCurve25519.generatePublicKey(clampedPriv)
        val fingerprintHex = Sha256Digest.digestHex(pubKey)
        val safetyNumber = formatSafetyNumber(Sha256Digest.digest(pubKey))

        // 2. Ed25519 Signing KeyPair (F0: Proof-of-Possession)
        val rawSigningPriv = Argon2Kmp.deriveKey(seed = seed, salt = SIGNING_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val signingPubKey = IdentityEd25519.generatePublicKey(rawSigningPriv)

        return IdentityKeyPair(
            privateKey = clampedPriv,
            publicKey = pubKey,
            fingerprintHex = fingerprintHex,
            safetyNumber = safetyNumber,
            signingPrivateKey = rawSigningPriv,
            signingPublicKey = signingPubKey
        )
    }

    fun buildRoutingSignaturePayload(fingerprint: String, newAuthUid: String, timestamp: Long): String {
        return "pmsg-routing-v1|$fingerprint|$newAuthUid|$timestamp"
    }

    fun signRoutingUpdate(signingPrivKeySeed: ByteArray, fingerprint: String, newAuthUid: String, timestamp: Long): ByteArray {
        val payload = buildRoutingSignaturePayload(fingerprint, newAuthUid, timestamp).encodeToByteArray()
        return IdentityEd25519.sign(signingPrivKeySeed, payload)
    }

    fun verifyRoutingUpdate(signingPubKey: ByteArray, fingerprint: String, newAuthUid: String, timestamp: Long, signature: ByteArray): Boolean {
        val payload = buildRoutingSignaturePayload(fingerprint, newAuthUid, timestamp).encodeToByteArray()
        return IdentityEd25519.verify(signingPubKey, payload, signature)
    }

    fun restoreFromMnemonic(mnemonic: List<String>): Result<IdentityKeyPair> {
        val validationResult = Bip39Portuguese.mnemonicToEntropy(mnemonic)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull() ?: IllegalArgumentException("Mnemônico inválido"))
        }
        return try {
            Result.success(deriveKeyPair(mnemonic))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats a 32-byte hash into Signal-style Safety Number:
     * Exactly 12 blocks of 5 decimal digits (60 digits total).
     */
    fun formatSafetyNumber(hash: ByteArray): String {
        // Expand 32 bytes into 48 bytes (12 * 4 bytes)
        val part1 = Sha256Digest.digest(hash + byteArrayOf(0x01))
        val part2 = Sha256Digest.digest(hash + byteArrayOf(0x02))
        val stream = part1 + part2

        val blocks = ArrayList<String>(12)
        for (i in 0 until 12) {
            val offset = i * 4
            val val32 = ((stream[offset].toLong() and 0xFF) shl 24) or
                    ((stream[offset + 1].toLong() and 0xFF) shl 16) or
                    ((stream[offset + 2].toLong() and 0xFF) shl 8) or
                    (stream[offset + 3].toLong() and 0xFF)
            val fiveDigit = (val32 % 100000L).toString().padStart(5, '0')
            blocks.add(fiveDigit)
        }
        return blocks.joinToString(" ")
    }

    /**
     * Computes the shared Pair Safety Number between two parties (Alice and Bob).
     * Symmetrical: order of keys does not matter. Both parties derive the exact same 60 digits.
     */
    fun computePairSafetyNumber(myPubKey: ByteArray, peerPubKey: ByteArray): String {
        require(myPubKey.size == 32) { "myPubKey must be 32 bytes" }
        require(peerPubKey.size == 32) { "peerPubKey must be 32 bytes" }

        val (first, second) = if (compareBytes(myPubKey, peerPubKey) <= 0) {
            myPubKey to peerPubKey
        } else {
            peerPubKey to myPubKey
        }

        val combined = ByteArray(64)
        first.copyInto(combined, 0, 0, 32)
        second.copyInto(combined, 32, 0, 32)

        val hash = Sha256Digest.digest(combined)
        return formatSafetyNumber(hash)
    }

    fun compareBytes(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val vA = a[i].toInt() and 0xFF
            val vB = b[i].toInt() and 0xFF
            if (vA != vB) return vA.compareTo(vB)
        }
        return a.size.compareTo(b.size)
    }

    /**
     * Envelope encryption: encrypts the raw X25519 private key using the hardware KeyVault.
     */
    fun envelopeEncrypt(data: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val hex = StringBuilder(data.size * 2)
        for (b in data) {
            val v = b.toInt() and 0xFF
            hex.append(hexChars[v ushr 4])
            hex.append(hexChars[v and 0x0F])
        }
        return KeyVault.encrypt(hex.toString())
    }

    /**
     * Envelope decryption: decrypts the ciphertext using KeyVault to recover raw bytes in RAM.
     */
    fun envelopeDecrypt(cipherText: String): ByteArray {
        val hex = KeyVault.decrypt(cipherText)
        if (hex.startsWith("🔒") || hex.length % 2 != 0) {
            return ByteArray(0)
        }
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val high = hex[i * 2].digitToIntOrNull(16) ?: return ByteArray(0)
            val low = hex[i * 2 + 1].digitToIntOrNull(16) ?: return ByteArray(0)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
