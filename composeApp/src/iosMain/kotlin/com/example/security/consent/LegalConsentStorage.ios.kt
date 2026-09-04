package com.example.security.consent

actual object LegalConsentStorage {
    private var inMemoryCache: LegalConsentRecord? = null

    actual fun getConsent(): LegalConsentRecord? = inMemoryCache
    actual fun saveConsent(consent: LegalConsentRecord) { inMemoryCache = consent }
    actual fun clearConsent() { inMemoryCache = null }
}
