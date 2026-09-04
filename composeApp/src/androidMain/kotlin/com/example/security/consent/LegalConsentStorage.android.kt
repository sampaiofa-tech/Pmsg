package com.example.security.consent

import android.content.Context
import android.content.SharedPreferences

actual object LegalConsentStorage {

    private const val PREFS_NAME = "pmsg_legal_consent_store_v1"
    private var appContext: Context? = null

    @Volatile
    private var inMemoryCache: LegalConsentRecord? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun getConsent(): LegalConsentRecord? {
        inMemoryCache?.let { return it }
        val prefs = getPrefs() ?: return null
        if (!prefs.contains("version") || !prefs.contains("acceptedAt")) return null
        val version = prefs.getString("version", "") ?: ""
        val acceptedAt = prefs.getLong("acceptedAt", 0L)
        val confirmedAge18 = prefs.getBoolean("confirmedAge18", false)
        return LegalConsentRecord(version, acceptedAt, confirmedAge18).also { inMemoryCache = it }
    }

    actual fun saveConsent(consent: LegalConsentRecord) {
        inMemoryCache = consent
        val prefs = getPrefs() ?: return
        prefs.edit()
            .putString("version", consent.version)
            .putLong("acceptedAt", consent.acceptedAt)
            .putBoolean("confirmedAge18", consent.confirmedAge18)
            .apply()
    }

    actual fun clearConsent() {
        inMemoryCache = null
        getPrefs()?.edit()?.clear()?.apply()
    }
}
