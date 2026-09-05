package com.example.security.identity

import android.content.Context
import android.content.SharedPreferences

actual object IdentityStorage {

    private const val PREFS_NAME = "pmsg_identity_store_v1"
    private var appContext: Context? = null

    @Volatile
    private var inMemoryCache: StoredIdentity? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(): SharedPreferences? {
        val ctx = appContext ?: com.example.util.AndroidContextHolder.appContext
        return ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun hasIdentity(): Boolean {
        if (inMemoryCache != null) return true
        val prefs = getPrefs() ?: return false
        return prefs.contains("publicKeyBase64") && prefs.contains("encryptedPrivateKey")
    }

    actual fun saveIdentity(identity: StoredIdentity) {
        inMemoryCache = identity
        val prefs = getPrefs() ?: return
        prefs.edit()
            .putString("publicKeyBase64", identity.publicKeyBase64)
            .putString("fingerprintHex", identity.fingerprintHex)
            .putString("safetyNumber", identity.safetyNumber)
            .putString("encryptedPrivateKey", identity.encryptedPrivateKey)
            .putString("encryptedEntropy", identity.encryptedEntropy)
            .putString("signingPublicKeyBase64", identity.signingPublicKeyBase64)
            .putString("encryptedSigningPrivateKey", identity.encryptedSigningPrivateKey)
            .apply()
    }

    actual fun getIdentity(): StoredIdentity? {
        inMemoryCache?.let { return it }
        val prefs = getPrefs() ?: return null
        val pk = prefs.getString("publicKeyBase64", null) ?: return null
        val fp = prefs.getString("fingerprintHex", null) ?: return null
        val sn = prefs.getString("safetyNumber", null) ?: return null
        val epk = prefs.getString("encryptedPrivateKey", null) ?: return null
        val ee = prefs.getString("encryptedEntropy", null) ?: return null
        val spk = prefs.getString("signingPublicKeyBase64", "") ?: ""
        val espk = prefs.getString("encryptedSigningPrivateKey", "") ?: ""
        return StoredIdentity(pk, fp, sn, epk, ee, spk, espk).also { inMemoryCache = it }
    }

    actual fun clearIdentity() {
        inMemoryCache = null
        getPrefs()?.edit()?.clear()?.apply()
    }
}
