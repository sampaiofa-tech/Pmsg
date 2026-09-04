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
data class StoreKeyResult(
    val success: Boolean,
    val messageId: String? = null,
    val expiresAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Client service to persist Data Encryption Keys (DEKs) via the server-side
 * HTTPS Callable function 'storeMessageKey'.
 *
 * Adheres strictly to the Firebase Callable Protocol:
 * - Method: POST
 * - Content-Type: application/json
 * - Authorization: Bearer <FIREBASE_ID_TOKEN>
 * - Body: Wrapped in {"data": { ... }} envelope
 */
object KeyStoreClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun storeMessageKey(
        messageId: String,
        senderId: String,
        recipientId: String,
        ephemeralPubKey: String,
        wrappedDek: String,
        expiresAtMillis: Long,
        idToken: String
    ): StoreKeyResult {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("messageId", messageId)
                    put("senderId", senderId)
                    put("recipientId", recipientId)
                    put("ephemeralPubKey", ephemeralPubKey)
                    put("wrappedDek", wrappedDek)
                    put("expiresAtMillis", expiresAtMillis)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.storeMessageKeyUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                val success = resultObj?.get("success")?.jsonPrimitive?.content?.toBoolean() ?: true
                val returnedMsgId = resultObj?.get("messageId")?.jsonPrimitive?.content ?: messageId
                val returnedExpiresAt = resultObj?.get("expiresAt")?.jsonPrimitive?.content?.toLongOrNull() ?: expiresAtMillis

                StoreKeyResult(
                    success = success,
                    messageId = returnedMsgId,
                    expiresAt = returnedExpiresAt
                )
            } else {
                val errorMsg = try {
                    val parsed = json.parseToJsonElement(responseBody).jsonObject
                    parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: "HTTP ${response.status.value}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.status.value}: $responseBody"
                }

                StoreKeyResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            StoreKeyResult(
                success = false,
                errorMessage = "Network exception: ${e.message}"
            )
        }
    }

    suspend fun getMessageKey(
        messageId: String,
        idToken: String
    ): GetKeyResult {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("messageId", messageId)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.getMessageKeyUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                val success = resultObj?.get("success")?.jsonPrimitive?.content?.toBoolean() ?: true
                val returnedMsgId = resultObj?.get("messageId")?.jsonPrimitive?.content ?: messageId
                val ephemeralPubKey = resultObj?.get("ephemeralPubKey")?.jsonPrimitive?.content
                val wrappedDek = resultObj?.get("wrappedDek")?.jsonPrimitive?.content
                val returnedExpiresAt = resultObj?.get("expiresAtMillis")?.jsonPrimitive?.content?.toLongOrNull()

                if (ephemeralPubKey != null && wrappedDek != null) {
                    GetKeyResult(
                        success = success,
                        messageId = returnedMsgId,
                        ephemeralPubKey = ephemeralPubKey,
                        wrappedDek = wrappedDek,
                        expiresAtMillis = returnedExpiresAt
                    )
                } else {
                    GetKeyResult(
                        success = false,
                        errorMessage = "Malformed response: missing opaque wrapped DEK payload"
                    )
                }
            } else {
                val errorMsg = try {
                    val parsed = json.parseToJsonElement(responseBody).jsonObject
                    parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: "HTTP ${response.status.value}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.status.value}: $responseBody"
                }

                GetKeyResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            GetKeyResult(
                success = false,
                errorMessage = "Network exception: ${e.message}"
            )
        }
    }
}

@Serializable
data class GetKeyResult(
    val success: Boolean,
    val messageId: String? = null,
    val ephemeralPubKey: String? = null,
    val wrappedDek: String? = null,
    val expiresAtMillis: Long? = null,
    val errorMessage: String? = null
)
