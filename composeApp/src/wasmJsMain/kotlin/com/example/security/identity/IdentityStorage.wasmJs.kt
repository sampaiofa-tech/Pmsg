package com.example.security.identity

actual object IdentityStorage {
    @Volatile
    private var inMemoryCache: StoredIdentity? = null

    actual fun hasIdentity(): Boolean = inMemoryCache != null
    actual fun saveIdentity(identity: StoredIdentity) { inMemoryCache = identity }
    actual fun getIdentity(): StoredIdentity? = inMemoryCache
    actual fun clearIdentity() { inMemoryCache = null }
}
