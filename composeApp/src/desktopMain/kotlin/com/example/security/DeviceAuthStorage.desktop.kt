package com.example.security

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.serialization.json.Json
import java.io.File

actual object DeviceAuthStorage {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val authStorageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "auth_session.dpapi")
    }

    actual fun loadSession(): StoredAuthSession? {
        if (!authStorageFile.exists()) return null
        return try {
            val rawJson = if (Platform.isWindows()) {
                val encrypted = authStorageFile.readBytes()
                val decrypted = Crypt32Util.cryptUnprotectData(encrypted)
                String(decrypted, Charsets.UTF_8)
            } else {
                authStorageFile.readText()
            }
            json.decodeFromString(StoredAuthSession.serializer(), rawJson)
        } catch (_: Exception) {
            null
        }
    }

    actual fun saveSession(session: StoredAuthSession) {
        try {
            val rawJson = json.encodeToString(StoredAuthSession.serializer(), session)
            if (Platform.isWindows()) {
                val encrypted = Crypt32Util.cryptProtectData(rawJson.toByteArray(Charsets.UTF_8))
                authStorageFile.writeBytes(encrypted)
            } else {
                authStorageFile.writeText(rawJson)
            }
        } catch (_: Exception) {
            // Fallback silencioso
        }
    }

    actual fun clearSession() {
        if (authStorageFile.exists()) {
            try {
                authStorageFile.delete()
            } catch (_: Exception) {}
        }
    }
}
