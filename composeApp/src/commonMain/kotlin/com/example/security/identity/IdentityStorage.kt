package com.example.security.identity

data class StoredIdentity(
    val publicKeyBase64: String,
    val fingerprintHex: String,
    val safetyNumber: String,
    val encryptedPrivateKey: String,
    val encryptedEntropy: String
)

/**
 * Multiplatform persistent storage for the device cryptographic identity.
 * Stores only public data and hardware-encrypted envelopes (KeyVault).
 * Plaintext private keys and plain mnemonics are NEVER persisted.
 */
expect object IdentityStorage {
    fun hasIdentity(): Boolean
    fun saveIdentity(identity: StoredIdentity)
    fun getIdentity(): StoredIdentity?
    fun clearIdentity()
}
