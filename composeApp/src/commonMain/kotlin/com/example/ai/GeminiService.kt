package com.example.ai

/**
 * Multiplatform AI Service (Gemini Private Burner Notes & Ephemeral Summaries).
 *
 * Strategies:
 * - Android: Native Firebase AI Logic (firebase-ai / GoogleGenAI) with Play Integrity App Check.
 * - Desktop: REST API Client (Ktor) targeting Google AI Studio endpoint.
 * - iOS: REST API Client / Firebase iOS SDK with DeviceCheck App Attest.
 * - Web (WasmJS): REST API Client (Ktor) with reCAPTCHA Enterprise verification.
 */
expect class GeminiService() {
    suspend fun generateEphemeralBurnerNote(prompt: String): String
}
