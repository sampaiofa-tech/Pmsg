package com.example.security

data class IntegrityResult(
    val isCompromised: Boolean,
    val isEmulator: Boolean = false,
    val isDebuggerAttached: Boolean = false,
    val summary: String = "Dispositivo integro"
)

/**
 * Multiplatform Device Integrity & Tampering Auditor.
 *
 * Implementations:
 * - Android: Root detection (RootBeer / su / test-keys), emulator detection, debugger attachment.
 * - iOS: Jailbreak detection (Cydia, MobileSubstrate, file sandbox violations), debugging ptrace.
 * - Desktop: Debugger detection, elevated admin process warnings.
 * - Web (WasmJS): DevTools open detection, non-secure context (HTTP) warnings.
 */
expect object DeviceIntegrity {
    fun checkIntegrity(): IntegrityResult
}
