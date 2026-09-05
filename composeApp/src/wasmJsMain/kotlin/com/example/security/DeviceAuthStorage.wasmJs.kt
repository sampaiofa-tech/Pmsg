package com.example.security

actual object DeviceAuthStorage {
    private var inMemoryCache: StoredAuthSession? = null

    actual fun loadSession(): StoredAuthSession? = inMemoryCache
    actual fun saveSession(session: StoredAuthSession) { inMemoryCache = session }
    actual fun clearSession() { inMemoryCache = null }
}
