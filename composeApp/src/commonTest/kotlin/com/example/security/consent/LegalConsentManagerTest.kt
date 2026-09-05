package com.example.security.consent

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LegalConsentManagerTest {

    @BeforeTest
    fun setUp() {
        LegalConsentManager.clearConsent()
    }

    @AfterTest
    fun tearDown() {
        LegalConsentManager.clearConsent()
    }

    @Test
    fun testInitialState_isConsentInvalid() {
        assertFalse(
            LegalConsentManager.isConsentValid(),
            "O consentimento inicial deve ser inválido sem ação do usuário."
        )
    }

    @Test
    fun testRefusal_doesNotRecordAndRemainsInvalid() {
        val recorded = LegalConsentManager.recordConsent(confirmedAge18 = false)
        assertFalse(recorded, "Consentimento com menor de 18 anos deve ser rejeitado.")
        assertFalse(
            LegalConsentManager.isConsentValid(),
            "O app deve permanecer bloqueado para menores de 18 anos."
        )
    }

    @Test
    fun testAcceptance_recordsCorrectVersionAndAdvances() {
        val recorded = LegalConsentManager.recordConsent(confirmedAge18 = true)
        assertTrue(recorded, "Aceite de 18+ e Termos deve ser registrado com sucesso.")
        assertTrue(
            LegalConsentManager.isConsentValid(),
            "Consentimento válido deve permitir o avanço no app."
        )

        val record = LegalConsentManager.getConsent()
        assertNotNull(record)
        assertEquals(LegalConsentManager.CURRENT_LEGAL_VERSION, record.version)
        assertTrue(record.confirmedAge18)
    }

    @Test
    fun testOutdatedLegalVersion_requiresReacceptance() {
        // Simula aceite de uma versão anterior dos termos/política (ex: v0.9)
        val outdated = LegalConsentRecord(
            version = "0.9",
            acceptedAt = 1000L,
            confirmedAge18 = true
        )
        LegalConsentStorage.saveConsent(outdated)

        assertFalse(
            LegalConsentManager.isConsentValid(),
            "Versão desatualizada dos termos deve invalidar o consentimento e exigir re-aceite."
        )

        // Usuário aceita a nova versão v3.0
        LegalConsentManager.recordConsent(confirmedAge18 = true)
        assertTrue(
            LegalConsentManager.isConsentValid(),
            "Após re-aceite da versão atual, o consentimento volta a ser válido."
        )
    }

    @Test
    fun testClearConsent_invalidatesAccess() {
        LegalConsentManager.recordConsent(confirmedAge18 = true)
        assertTrue(LegalConsentManager.isConsentValid())

        LegalConsentManager.clearConsent()
        assertFalse(
            LegalConsentManager.isConsentValid(),
            "Ao limpar os dados (ex: Panic Wipe), o consentimento deve ser revogado."
        )
    }
}
