package com.example.security

import kotlinx.serialization.Serializable

@Serializable
data class StoredAuthSession(
    val userId: String,
    val idToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
)

/**
 * Multiplatform persistent storage for anonymous device auth sessions.
 * On Windows/Desktop: Protected via Windows DPAPI.
 * On Android: Stored in App SharedPreferences.
 * On iOS/WasmJs: Stored in platform keychain / memory.
 */
expect object DeviceAuthStorage {
    fun loadSession(): StoredAuthSession?
    fun saveSession(session: StoredAuthSession)
    fun clearSession()
}
