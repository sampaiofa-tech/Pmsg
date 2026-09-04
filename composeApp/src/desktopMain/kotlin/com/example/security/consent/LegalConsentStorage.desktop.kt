package com.example.security.consent

import java.io.File
import java.util.Properties

actual object LegalConsentStorage {

    private val storageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "consent_v1.properties")
    }

    @Volatile
    private var inMemoryCache: LegalConsentRecord? = null

    actual fun getConsent(): LegalConsentRecord? {
        inMemoryCache?.let { return it }
        if (!storageFile.exists()) return null
        return try {
            val props = Properties()
            storageFile.inputStream().use { props.load(it) }
            val version = props.getProperty("version") ?: return null
            val acceptedAt = props.getProperty("acceptedAt")?.toLongOrNull() ?: return null
            val confirmedAge18 = props.getProperty("confirmedAge18")?.toBooleanStrictOrNull() ?: false
            LegalConsentRecord(version, acceptedAt, confirmedAge18).also { inMemoryCache = it }
        } catch (_: Exception) {
            null
        }
    }

    actual fun saveConsent(consent: LegalConsentRecord) {
        inMemoryCache = consent
        try {
            val props = Properties()
            props.setProperty("version", consent.version)
            props.setProperty("acceptedAt", consent.acceptedAt.toString())
            props.setProperty("confirmedAge18", consent.confirmedAge18.toString())
            storageFile.outputStream().use { props.store(it, "Pmsg Legal Consent") }
        } catch (_: Exception) {
            // Permanece em cache volátil em caso de falha de I/O
        }
    }

    actual fun clearConsent() {
        inMemoryCache = null
        if (storageFile.exists()) {
            try {
                storageFile.delete()
            } catch (_: Exception) {}
        }
    }
}
