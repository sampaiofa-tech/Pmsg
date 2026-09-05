package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.util.AndroidContextHolder

actual object DeviceAuthStorage {
    private const val PREFS_NAME = "pmsg_device_auth_v1"
    private const val KEY_USER_ID = "userId"
    private const val KEY_ID_TOKEN = "idToken"
    private const val KEY_REFRESH_TOKEN = "refreshToken"
    private const val KEY_EXPIRES_AT = "expiresAtMillis"

    @Volatile
    private var inMemoryCache: StoredAuthSession? = null

    private fun getPrefs(): SharedPreferences? {
        return AndroidContextHolder.appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadSession(): StoredAuthSession? {
        inMemoryCache?.let { return it }
        val prefs = getPrefs() ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val idToken = prefs.getString(KEY_ID_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return StoredAuthSession(userId, idToken, refreshToken, expiresAt).also { inMemoryCache = it }
    }

    actual fun saveSession(session: StoredAuthSession) {
        inMemoryCache = session
        val prefs = getPrefs() ?: return
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ID_TOKEN, session.idToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .apply()
    }

    actual fun clearSession() {
        inMemoryCache = null
        getPrefs()?.edit()?.clear()?.apply()
    }
}
