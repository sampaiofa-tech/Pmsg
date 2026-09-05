package com.example.security

import com.example.data.network.ApiClient
import com.example.data.network.AppEndpoints
import com.example.data.network.PlatformEnvironment
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Unified Multiplatform Device Authentication Manager.
 *
 * Implements Zero-Trace Device-Bound Authentication via Firebase Auth (Identity Toolkit REST API).
 * Operates anonymously without collecting any PII (no phone, no email, no personal data).
 * The assigned cryptographic UID ('localId') is used as 'senderId' / 'recipientId' for routing.
 */
object DeviceAuthManager {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()

    @Volatile
    private var cachedSession: StoredAuthSession? = null

    /**
     * Returns the unique device/user UID authenticated in Firebase.
     * Guaranteed to match the 'senderId' in all message transactions.
     */
    fun getUserId(): String {
        cachedSession?.let { return it.userId }
        DeviceAuthStorage.loadSession()?.let {
            cachedSession = it
            return it.userId
        }
        return "device_${PlatformEnvironment.currentTimeMillis()}"
    }

    /**
     * Retrieves or refreshes a valid Firebase ID Token (JWT).
     * If token expires within 5 minutes, automatically refreshes via refreshToken.
     */
    suspend fun getIdToken(): String? = mutex.withLock {
        val now = PlatformEnvironment.currentTimeMillis()
        val current = cachedSession ?: DeviceAuthStorage.loadSession()

        if (current != null && (current.expiresAtMillis - now) > 5 * 60 * 1000L) {
            cachedSession = current
            return current.idToken
        }

        // Try refresh token if present
        if (current != null && current.refreshToken.isNotBlank()) {
            val refreshed = refreshIdToken(current.refreshToken, current.userId)
            if (refreshed != null) {
                cachedSession = refreshed
                DeviceAuthStorage.saveSession(refreshed)
                return refreshed.idToken
            }
        }

        // Otherwise perform anonymous device registration
        val newSession = performAnonymousSignUp()
        if (newSession != null) {
            cachedSession = newSession
            DeviceAuthStorage.saveSession(newSession)
            return newSession.idToken
        }

        // Fallback for debug/emulator
        return if (PlatformEnvironment.isDebug) {
            current?.idToken ?: "PMSG_DEV_TOKEN_${PlatformEnvironment.currentTimeMillis()}"
        } else {
            null
        }
    }

    suspend fun ensureAuthenticated(): Pair<String, String> {
        val token = getIdToken() ?: throw IllegalStateException("Falha ao inicializar autenticação anônima com o servidor.")
        val uid = getUserId()
        return Pair(uid, token)
    }

    private suspend fun performAnonymousSignUp(): StoredAuthSession? {
        return try {
            val apiKey = AppEndpoints.webApiKey
            val url = "${AppEndpoints.identityToolkitBaseUrl}/accounts:signUp?key=$apiKey"
            val payload = buildJsonObject {
                put("returnSecureToken", true)
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsed = json.parseToJsonElement(body).jsonObject

                val idToken = parsed["idToken"]?.jsonPrimitive?.content ?: return null
                val refreshToken = parsed["refreshToken"]?.jsonPrimitive?.content ?: ""
                val localId = parsed["localId"]?.jsonPrimitive?.content
                    ?: "device_${PlatformEnvironment.currentTimeMillis()}"
                val expiresInSec = parsed["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
                val expiresAt = PlatformEnvironment.currentTimeMillis() + (expiresInSec * 1000L)

                StoredAuthSession(
                    userId = localId,
                    idToken = idToken,
                    refreshToken = refreshToken,
                    expiresAtMillis = expiresAt
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun refreshIdToken(refreshToken: String, userId: String): StoredAuthSession? {
        return try {
            val apiKey = AppEndpoints.webApiKey
            val url = "${AppEndpoints.secureTokenBaseUrl}/token?key=$apiKey"

            val payload = buildJsonObject {
                put("grant_type", "refresh_token")
                put("refresh_token", refreshToken)
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsed = json.parseToJsonElement(body).jsonObject

                val idToken = parsed["id_token"]?.jsonPrimitive?.content
                    ?: parsed["idToken"]?.jsonPrimitive?.content
                    ?: return null
                val newRefreshToken = parsed["refresh_token"]?.jsonPrimitive?.content
                    ?: parsed["refreshToken"]?.jsonPrimitive?.content
                    ?: refreshToken
                val returnedUserId = parsed["user_id"]?.jsonPrimitive?.content ?: userId
                val expiresInSec = parsed["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
                val expiresAt = PlatformEnvironment.currentTimeMillis() + (expiresInSec * 1000L)

                StoredAuthSession(
                    userId = returnedUserId,
                    idToken = idToken,
                    refreshToken = newRefreshToken,
                    expiresAtMillis = expiresAt
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearSession() {
        cachedSession = null
        DeviceAuthStorage.clearSession()
    }
}
