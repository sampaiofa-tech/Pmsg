package com.example.security

/**
 * Multiplatform App Check & Endpoint Attestation Verifier.
 *
 * ARCHITECTURAL SPECIFICATION:
 * - Mobile (Android): Hardware-backed Google Play Integrity API with debug token fallback.
 * - Mobile (iOS): Apple App Attest / DeviceCheck token via Firebase App Check SDK.
 * - Desktop (Windows/JVM): reCAPTCHA Enterprise REST API verification token exchange.
 * - Web (WasmJS): reCAPTCHA Enterprise Web provider. Note: Web clients are inherently
 *   subject to DevTools manipulation and represent lowest trust tier.
 */
expect object AppCheckVerifier {
    suspend fun getAttestationToken(): String?
}
