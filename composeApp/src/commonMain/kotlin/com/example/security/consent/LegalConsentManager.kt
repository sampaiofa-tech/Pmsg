package com.example.security.consent

/**
 * Manages the versioned legal agreement (Privacy Policy + Terms of Use) and 18+ age verification.
 * In accordance with Google Play User Data policies, users must explicitly confirm they are 18+
 * and accept the current legal documents before accessing any messaging features.
 */
object LegalConsentManager {

    const val CURRENT_LEGAL_VERSION = "1.0"

    fun isConsentValid(): Boolean {
        val record = LegalConsentStorage.getConsent() ?: return false
        return record.confirmedAge18 && record.version == CURRENT_LEGAL_VERSION
    }

    fun recordConsent(confirmedAge18: Boolean, timestamp: Long = currentTimestamp()): Boolean {
        if (!confirmedAge18) return false
        val record = LegalConsentRecord(
            version = CURRENT_LEGAL_VERSION,
            acceptedAt = timestamp,
            confirmedAge18 = true
        )
        LegalConsentStorage.saveConsent(record)
        return true
    }

    fun getConsent(): LegalConsentRecord? {
        return LegalConsentStorage.getConsent()
    }

    fun clearConsent() {
        LegalConsentStorage.clearConsent()
    }

    private fun currentTimestamp(): Long {
        return try {
            com.example.data.network.PlatformEnvironment.currentTimeMillis()
        } catch (_: Throwable) {
            0L
        }
    }
}
