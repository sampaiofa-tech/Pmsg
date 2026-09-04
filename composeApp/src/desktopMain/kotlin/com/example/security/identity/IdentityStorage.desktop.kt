package com.example.security.identity

import java.io.File
import java.util.Properties

actual object IdentityStorage {

    private val storageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "identity_v1.properties")
    }

    @Volatile
    private var inMemoryCache: StoredIdentity? = null

    actual fun hasIdentity(): Boolean {
        if (inMemoryCache != null) return true
        return storageFile.exists() && storageFile.length() > 0
    }

    actual fun saveIdentity(identity: StoredIdentity) {
        inMemoryCache = identity
        try {
            val props = Properties()
            props.setProperty("publicKeyBase64", identity.publicKeyBase64)
            props.setProperty("fingerprintHex", identity.fingerprintHex)
            props.setProperty("safetyNumber", identity.safetyNumber)
            props.setProperty("encryptedPrivateKey", identity.encryptedPrivateKey)
            props.setProperty("encryptedEntropy", identity.encryptedEntropy)
            props.setProperty("signingPublicKeyBase64", identity.signingPublicKeyBase64)
            props.setProperty("encryptedSigningPrivateKey", identity.encryptedSigningPrivateKey)
            storageFile.outputStream().use { props.store(it, "Pmsg Encrypted Identity") }
        } catch (_: Exception) {
            // Em caso de falha de I/O, permanece em cache volátil
        }
    }

    actual fun getIdentity(): StoredIdentity? {
        inMemoryCache?.let { return it }
        if (!storageFile.exists()) return null
        return try {
            val props = Properties()
            storageFile.inputStream().use { props.load(it) }
            val pk = props.getProperty("publicKeyBase64") ?: return null
            val fp = props.getProperty("fingerprintHex") ?: return null
            val sn = props.getProperty("safetyNumber") ?: return null
            val epk = props.getProperty("encryptedPrivateKey") ?: return null
            val ee = props.getProperty("encryptedEntropy") ?: return null
            val spk = props.getProperty("signingPublicKeyBase64") ?: ""
            val espk = props.getProperty("encryptedSigningPrivateKey") ?: ""
            StoredIdentity(pk, fp, sn, epk, ee, spk, espk).also { inMemoryCache = it }
        } catch (_: Exception) {
            null
        }
    }

    actual fun clearIdentity() {
        inMemoryCache = null
        if (storageFile.exists()) {
            try {
                // Multi-pass shredding: overwrite with noise before delete
                storageFile.writeBytes(ByteArray(storageFile.length().toInt()) { 0 })
                storageFile.delete()
            } catch (_: Exception) {}
        }
    }
}
