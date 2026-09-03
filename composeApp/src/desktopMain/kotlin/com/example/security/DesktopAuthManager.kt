package com.example.security

import com.example.data.network.ApiClient
import com.example.data.network.AppEndpoints
import com.example.data.network.PlatformEnvironment
import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File

@Serializable
data class StoredAuthSession(
    val userId: String,
    val idToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
)

/**
 * Desktop Authentication and Device Identity Manager.
 *
 * Implements Zero-Trace Device-Bound Authentication via Firebase Auth (Identity Toolkit REST API).
 *
 * SECURITY & PRIVACY ARCHITECTURE:
 * 1. Anonymous Device Identity: Generates and persists an ephemeral cryptographic UID ('localId')
 *    without requiring PII (Personally Identifiable Information) such as phone or email.
 * 2. Hardware/OS Protection: Session tokens and keys are encrypted at rest using Windows DPAPI
 *    (Crypt32Util.cryptProtectData).
 * 3. Consistent Sender Identity: The authenticated Firebase Auth UID matches 'senderId' in all
 *    messages and DEK storage requests ('storeMessageKey'), satisfying backend authorization rules.
 */
object DesktopAuthManager {

    private const val DEFAULT_DEV_WEB_API_KEY = "PMSG_DEV_WEB_API_KEY"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()

    @Volatile
    private var cachedSession: StoredAuthSession? = null

    private val authStorageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "auth_session.dpapi")
    }

    fun getWebApiKey(): String {
        return if (PlatformEnvironment.isDebug) {
            PlatformEnvironment.getEnv("FIREBASE_WEB_API_KEY")
                ?: System.getProperty("pmsg.webApiKey")
                ?: DEFAULT_DEV_WEB_API_KEY
        } else {
            // Em Release: Web API Key configurada na compilação
            System.getProperty("pmsg.webApiKey") ?: DEFAULT_DEV_WEB_API_KEY
        }
    }

    /**
     * Retorna o ID único do usuário/dispositivo autenticado no Firebase.
     * Este UID DEVE ser usado como 'senderId' nas mensagens para atender
     * ao requisito de autorização do backend (auth.uid == senderId).
     */
    fun getUserId(): String {
        cachedSession?.let { return it.userId }
        loadPersistedSession()?.let { return it.userId }
        // Fallback transitório seguro antes do primeiro handshake de rede
        return "desktop_user_pending"
    }

    /**
     * Obtém um Firebase ID Token válido (criptograficamente assinado pelo Google).
     * Se o token estiver expirando em menos de 5 minutos, renova automaticamente via refreshToken.
     */
    suspend fun getIdToken(): String? = mutex.withLock {
        val now = System.currentTimeMillis()
        val current = cachedSession ?: loadPersistedSession()

        if (current != null && (current.expiresAtMillis - now) > 5 * 60 * 1000L) {
            cachedSession = current
            return current.idToken
        }

        // Se possuir refreshToken, tenta renovar
        if (current != null && current.refreshToken.isNotBlank()) {
            val refreshed = refreshIdToken(current.refreshToken, current.userId)
            if (refreshed != null) {
                cachedSession = refreshed
                persistSession(refreshed)
                return refreshed.idToken
            }
        }

        // Caso contrário, realiza novo registro anônimo de dispositivo
        val newSession = performAnonymousSignUp()
        if (newSession != null) {
            cachedSession = newSession
            persistSession(newSession)
            return newSession.idToken
        }

        // Se estiver em modo debug/emulador e offline, retorna fallback de desenvolvimento
        return if (PlatformEnvironment.isDebug) {
            current?.idToken ?: "PMSG_DEV_TOKEN_${System.currentTimeMillis()}"
        } else {
            null
        }
    }

    private suspend fun performAnonymousSignUp(): StoredAuthSession? {
        return try {
            val apiKey = getWebApiKey()
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
                val localId = parsed["localId"]?.jsonPrimitive?.content ?: "desktop_${System.currentTimeMillis()}"
                val expiresInSec = parsed["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
                val expiresAt = System.currentTimeMillis() + (expiresInSec * 1000L)

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
            val apiKey = getWebApiKey()
            // SecureToken endpoint para renovação
            val url = if (AppEndpoints.isEmulator) {
                "${AppEndpoints.identityToolkitBaseUrl}/token?key=$apiKey"
            } else {
                "https://securetoken.googleapis.com/v1/token?key=$apiKey"
            }

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
                val expiresAt = System.currentTimeMillis() + (expiresInSec * 1000L)

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

    private fun persistSession(session: StoredAuthSession) {
        try {
            val rawJson = json.encodeToString(StoredAuthSession.serializer(), session)
            if (Platform.isWindows()) {
                val encrypted = Crypt32Util.cryptProtectData(rawJson.toByteArray(Charsets.UTF_8))
                authStorageFile.writeBytes(encrypted)
            } else {
                authStorageFile.writeText(rawJson)
            }
        } catch (_: Exception) {
            // Falhas de IO locais não interrompem a execução em memória
        }
    }

    private fun loadPersistedSession(): StoredAuthSession? {
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
}
