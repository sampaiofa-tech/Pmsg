package com.example.data.network

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class CreateInviteResult(
    val inviteToken: String,
    val inviteLink: String,
    val expiresAtMillis: Long
)

@Serializable
data class AcceptInviteResult(
    val creatorFingerprint: String,
    val creatorPubKey: String
)

@Serializable
data class ResolveFingerprintResult(
    val currentAuthUid: String,
    val pubKey: String,
    val updatedAt: Long
)

/**
 * Client service to interact with server-side identity & invitation Cloud Functions:
 * - createInvite (Modelo C: 24h ephemeral single-use remote invite)
 * - acceptInvite (Modelo C: single-use acceptance with vanish-after-accept)
 * - resolveFingerprint (privacy-preserving technical directory)
 * - updateIdentityRouting (Recovery: binding new Auth UID to immutable fingerprint)
 */
object IdentityNetworkClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Parses a raw input string into a 64-char hex invite token.
     * Supports both direct tokens and URI scheme 'pmsg://invite?token=...'
     */
    fun parseInviteToken(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 64 && trimmed.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            return trimmed.lowercase()
        }
        if (trimmed.startsWith("pmsg://invite")) {
            val queryIndex = trimmed.indexOf('?')
            if (queryIndex != -1 && queryIndex < trimmed.length - 1) {
                val params = trimmed.substring(queryIndex + 1).split('&')
                for (param in params) {
                    val parts = param.split('=')
                    if (parts.size == 2 && (parts[0] == "token" || parts[0] == "i")) {
                        val token = parts[1].trim().lowercase()
                        if (token.length == 64) return token
                    }
                }
            }
        }
        return null
    }

    suspend fun createInvite(
        creatorFingerprint: String,
        creatorPubKey: String,
        idToken: String
    ): Result<CreateInviteResult> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("creatorFingerprint", creatorFingerprint)
                    put("creatorPubKey", creatorPubKey)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.createInviteUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                    ?: return Result.failure(Exception("Resposta inválida do servidor de convites."))

                val token = resultObj["inviteToken"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Token de convite ausente."))
                val link = resultObj["inviteLink"]?.jsonPrimitive?.content
                    ?: "pmsg://invite?token=$token&fp=$creatorFingerprint"
                val expiresAt = resultObj["expiresAtMillis"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: (PlatformEnvironment.currentTimeMillis() + 86400000)

                Result.success(CreateInviteResult(token, link, expiresAt))
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(
        inviteToken: String,
        idToken: String
    ): Result<AcceptInviteResult> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("inviteToken", inviteToken)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.acceptInviteUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                    ?: return Result.failure(Exception("Resposta inválida ao aceitar convite."))

                val fp = resultObj["creatorFingerprint"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Fingerprint do criador ausente."))
                val pubKey = resultObj["creatorPubKey"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Chave pública do criador ausente."))

                Result.success(AcceptInviteResult(fp, pubKey))
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveFingerprint(
        fingerprint: String,
        idToken: String
    ): Result<ResolveFingerprintResult> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("fingerprint", fingerprint)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.resolveFingerprintUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                    ?: return Result.failure(Exception("Resposta inválida ao resolver fingerprint."))

                val uid = resultObj["currentAuthUid"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("UID técnico ausente."))
                val pubKey = resultObj["pubKey"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Chave pública ausente."))
                val updatedAt = resultObj["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: PlatformEnvironment.currentTimeMillis()

                Result.success(ResolveFingerprintResult(uid, pubKey, updatedAt))
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateIdentityRouting(
        fingerprint: String,
        pubKey: String,
        idToken: String
    ): Result<Boolean> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("fingerprint", fingerprint)
                    put("pubKey", pubKey)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.updateIdentityRoutingUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val parsed = json.parseToJsonElement(responseBody).jsonObject
            parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                ?: "HTTP $statusCode: $responseBody"
        } catch (_: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }
}
