package com.example.ui

import com.example.ui.screens.LegalConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegalScreenAndConstantsTest {

    @Test
    fun testLegalConstantsAdhereToDecisions() {
        assertEquals("Filippe Andrade Sampaio", LegalConstants.DPO_NAME)
        assertEquals("azfstick00@gmail.com", LegalConstants.DPO_EMAIL)
        assertEquals("https://sampaiofa-tech.github.io/Pmsg/privacidade.html", LegalConstants.POLICY_URL)
        assertEquals("https://sampaiofa-tech.github.io/Pmsg/termos.html", LegalConstants.TERMS_URL)
        assertEquals("us-central1 (EUA)", LegalConstants.SERVER_REGION)
        assertEquals(30, LegalConstants.LOG_RETENTION_DAYS)
    }

    @Test
    fun testUrlsAreHttpsAndPointingToGitHubPages() {
        assertTrue(LegalConstants.POLICY_URL.startsWith("https://sampaiofa-tech.github.io/Pmsg/"))
        assertTrue(LegalConstants.TERMS_URL.startsWith("https://sampaiofa-tech.github.io/Pmsg/"))
        assertTrue(LegalConstants.DPO_EMAIL.contains("@"))
    }
}
