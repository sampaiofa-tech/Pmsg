package com.example.security.consent

data class LegalConsentRecord(
    val version: String,
    val acceptedAt: Long,
    val confirmedAge18: Boolean
)

expect object LegalConsentStorage {
    fun getConsent(): LegalConsentRecord?
    fun saveConsent(consent: LegalConsentRecord)
    fun clearConsent()
}
