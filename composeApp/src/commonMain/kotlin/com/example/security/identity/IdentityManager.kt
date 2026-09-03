package com.example.security.identity

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ParsedContactPayload(
    val version: Int,
    val fingerprintHex: String,
    val publicKeyBytes: ByteArray,
    val publicKeyBase64: String,
    val authUid: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedContactPayload) return false
        return version == other.version &&
                fingerprintHex == other.fingerprintHex &&
                publicKeyBytes.contentEquals(other.publicKeyBytes) &&
                publicKeyBase64 == other.publicKeyBase64 &&
                authUid == other.authUid
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + fingerprintHex.hashCode()
        result = 31 * result + publicKeyBytes.contentHashCode()
        result = 31 * result + publicKeyBase64.hashCode()
        result = 31 * result + authUid.hashCode()
        return result
    }
}

/**
 * High-level coordinator for device cryptographic identity:
 * - Provisioning / initial 12-word display.
 * - Envelope encryption of private keys and seed material.
 * - Re-viewing mnemonic with PIN/biometrics.
 * - Generation and parsing of Model A contact exchange strings.
 */
@OptIn(ExperimentalEncodingApi::class)
object IdentityManager {

    fun hasIdentity(): Boolean = IdentityStorage.hasIdentity()

    fun getIdentity(): IdentityKeyPair? {
        val stored = IdentityStorage.getIdentity() ?: return null
        val privKey = IdentityCryptoManager.envelopeDecrypt(stored.encryptedPrivateKey)
        if (privKey.size != 32) return null
        val pubKey = Base64.decode(stored.publicKeyBase64)
        return IdentityKeyPair(
            privateKey = privKey,
            publicKey = pubKey,
            fingerprintHex = stored.fingerprintHex,
            safetyNumber = stored.safetyNumber
        )
    }

    fun getOrGenerateIdentity(): IdentityKeyPair {
        val existing = getIdentity()
        if (existing != null) return existing
        val provisioned = provisionNewIdentity()
        confirmAndSaveIdentity(provisioned)
        return provisioned.keyPair
    }

    fun provisionNewIdentity(): ProvisionedIdentity {
        return IdentityCryptoManager.generateNewIdentity()
    }

    fun confirmAndSaveIdentity(provisioned: ProvisionedIdentity) {
        val privEncrypted = IdentityCryptoManager.envelopeEncrypt(provisioned.keyPair.privateKey)
        val entropyEncrypted = IdentityCryptoManager.envelopeEncrypt(provisioned.entropy)
        val pubKeyBase64 = Base64.encode(provisioned.keyPair.publicKey)

        val stored = StoredIdentity(
            publicKeyBase64 = pubKeyBase64,
            fingerprintHex = provisioned.keyPair.fingerprintHex,
            safetyNumber = provisioned.keyPair.safetyNumber,
            encryptedPrivateKey = privEncrypted,
            encryptedEntropy = entropyEncrypted
        )
        IdentityStorage.saveIdentity(stored)
    }

    fun restoreFromMnemonic(words: List<String>): Result<IdentityKeyPair> {
        val restoreResult = IdentityCryptoManager.restoreFromMnemonic(words)
        if (restoreResult.isFailure) return restoreResult

        val keyPair = restoreResult.getOrThrow()
        val entropy = Bip39Portuguese.mnemonicToEntropy(words).getOrThrow()

        val privEncrypted = IdentityCryptoManager.envelopeEncrypt(keyPair.privateKey)
        val entropyEncrypted = IdentityCryptoManager.envelopeEncrypt(entropy)
        val pubKeyBase64 = Base64.encode(keyPair.publicKey)

        val stored = StoredIdentity(
            publicKeyBase64 = pubKeyBase64,
            fingerprintHex = keyPair.fingerprintHex,
            safetyNumber = keyPair.safetyNumber,
            encryptedPrivateKey = privEncrypted,
            encryptedEntropy = entropyEncrypted
        )
        IdentityStorage.saveIdentity(stored)
        return Result.success(keyPair)
    }

    fun getMnemonicWords(): List<String>? {
        val stored = IdentityStorage.getIdentity() ?: return null
        val entropy = IdentityCryptoManager.envelopeDecrypt(stored.encryptedEntropy)
        if (entropy.size != 16) return null
        return Bip39Portuguese.entropyToMnemonic(entropy)
    }

    fun clearIdentity() {
        IdentityStorage.clearIdentity()
    }

    /**
     * Generates Model A contact string:
     * pmsg://contact?v=1&fp=<fingerprintHex>&pk=<pubKeyBase64>&uid=<authUid>
     */
    fun createContactUri(authUid: String): String? {
        val identity = getIdentity() ?: return null
        val pkBase64 = Base64.encode(identity.publicKey)
        return "pmsg://contact?v=1&fp=${identity.fingerprintHex}&pk=$pkBase64&uid=$authUid"
    }

    /**
     * Parses and validates a Model A contact string:
     * Validates checksum and that SHA-256(decode(pk)) == fp.
     */
    fun parseContactUri(uri: String): Result<ParsedContactPayload> {
        val trimmed = uri.trim()
        if (!trimmed.startsWith("pmsg://contact?")) {
            return Result.failure(IllegalArgumentException("URI inválido. Deve iniciar com 'pmsg://contact?'"))
        }

        val query = trimmed.removePrefix("pmsg://contact?")
        val params = query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }

        val version = params["v"]?.toIntOrNull() ?: 1
        val fp = params["fp"]?.lowercase() ?: ""
        val pkBase64 = params["pk"] ?: ""
        val uid = params["uid"] ?: ""

        if (fp.length != 64 || !Regex("^[0-9a-f]{64}$").matches(fp)) {
            return Result.failure(IllegalArgumentException("Fingerprint inválido no código"))
        }
        if (pkBase64.isEmpty()) {
            return Result.failure(IllegalArgumentException("Chave pública ausente no código"))
        }
        if (uid.isEmpty()) {
            return Result.failure(IllegalArgumentException("UID ausente no código"))
        }

        val pkBytes = try {
            Base64.decode(pkBase64)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Chave pública malformada: ${e.message}"))
        }

        if (pkBytes.size != 32) {
            return Result.failure(IllegalArgumentException("Chave pública deve ter 32 bytes"))
        }

        val computedFp = Sha256Digest.digestHex(pkBytes)
        if (computedFp != fp) {
            return Result.failure(IllegalStateException("Inconsistência criptográfica: fingerprint não corresponde à chave pública!"))
        }

        return Result.success(
            ParsedContactPayload(
                version = version,
                fingerprintHex = fp,
                publicKeyBytes = pkBytes,
                publicKeyBase64 = pkBase64,
                authUid = uid
            )
        )
    }
}
