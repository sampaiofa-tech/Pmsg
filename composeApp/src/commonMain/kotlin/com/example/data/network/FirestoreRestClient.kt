package com.example.data.network

import com.example.data.model.FirestoreMessage
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Multiplatform Firestore REST Client for Zero-Trace Ephemeral Message synchronization.
 *
 * Implements direct HTTP interactions with Firestore REST API v1:
 * - Create: POST /messages?documentId={messageId}
 * - Query:  POST :runQuery (where recipientId == {myUid})
 * - Delete: DELETE /messages/{messageId} (triggers server-side crypto-shredding on read)
 *
 * Strictly adheres to firestore.rules and Zero-Knowledge principles.
 */
object FirestoreRestClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Publishes an encrypted message envelope to the Firestore 'messages' collection.
     * Enforces mandatory 'expiresAt' timestamp required by firestore.rules.
     */
    suspend fun createMessage(
        message: FirestoreMessage,
        idToken: String
    ): Result<Boolean> {
        return try {
            val isoTimestamp = Iso8601Utils.formatEpochToIso(message.expiresAt)
            val url = "${AppEndpoints.firestoreBaseUrl}/messages?documentId=${message.id}"

            val payload = buildJsonObject {
                putJsonObject("fields") {
                    putJsonObject("ciphertext") { put("stringValue", message.ciphertext) }
                    putJsonObject("iv") { put("stringValue", message.iv) }
                    putJsonObject("senderId") { put("stringValue", message.senderId) }
                    putJsonObject("recipientId") { put("stringValue", message.recipientId) }
                    putJsonObject("expiresAt") { put("timestampValue", isoTimestamp) }
                }
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(IllegalStateException("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries all pending unread messages addressed to [recipientId].
     */
    suspend fun fetchPendingMessages(
        recipientId: String,
        idToken: String
    ): Result<List<FirestoreMessage>> {
        return try {
            val url = "${AppEndpoints.firestoreBaseUrl}:runQuery"

            val payload = buildJsonObject {
                putJsonObject("structuredQuery") {
                    putJsonArray("from") {
                        addJsonObject { put("collectionId", "messages") }
                    }
                    putJsonObject("where") {
                        putJsonObject("fieldFilter") {
                            putJsonObject("field") { put("fieldPath", "recipientId") }
                            put("op", "EQUAL")
                            putJsonObject("value") { put("stringValue", recipientId) }
                        }
                    }
                }
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsedArray = json.parseToJsonElement(body).jsonArray
                val messages = mutableListOf<FirestoreMessage>()

                for (elem in parsedArray) {
                    val doc = elem.jsonObject["document"]?.jsonObject ?: continue
                    val name = doc["name"]?.jsonPrimitive?.content ?: continue
                    val messageId = name.substringAfterLast('/')

                    val fields = doc["fields"]?.jsonObject ?: continue
                    val ciphertext = fields["ciphertext"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val iv = fields["iv"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val senderId = fields["senderId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val returnedRecipient = fields["recipientId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val isoExpires = fields["expiresAt"]?.jsonObject?.get("timestampValue")?.jsonPrimitive?.content ?: ""
                    val expiresAt = Iso8601Utils.parseIsoToEpoch(isoExpires)

                    if (messageId.isNotBlank() && ciphertext.isNotBlank()) {
                        messages.add(
                            FirestoreMessage(
                                id = messageId,
                                ciphertext = ciphertext,
                                iv = iv,
                                senderId = senderId,
                                recipientId = returnedRecipient,
                                expiresAt = expiresAt
                            )
                        )
                    }
                }

                Result.success(messages)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(IllegalStateException("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a message from Firestore after client-side decryption (Vanish-After-Read).
     * Server-side trigger 'onDeleteMessage' will automatically shred the associated DEK in messageKeys.
     */
    suspend fun deleteMessage(
        messageId: String,
        idToken: String
    ): Result<Boolean> {
        return try {
            val url = "${AppEndpoints.firestoreBaseUrl}/messages/$messageId"
            val response = ApiClient.client.delete(url) {
                header("Authorization", "Bearer $idToken")
            }

            // HTTP 200 or 404 (already shredded) are considered success
            if (response.status.isSuccess() || response.status.value == 404) {
                Result.success(true)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(IllegalStateException("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Zero-dependency pure Kotlin RFC 3339 / ISO 8601 UTC Formatter and Parser.
 */
object Iso8601Utils {

    fun formatEpochToIso(epochMillis: Long): String {
        val seconds = epochMillis / 1000
        val millis = (epochMillis % 1000).toInt()
        var days = (seconds / 86400).toInt()
        var remSeconds = (seconds % 86400).toInt()
        if (remSeconds < 0) {
            remSeconds += 86400
            days -= 1
        }
        val hours = remSeconds / 3600
        val minutes = (remSeconds % 3600) / 60
        val secs = remSeconds % 60

        // Civil date algorithm (Howard Hinnant)
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        val year = y + (if (m <= 2) 1 else 0)

        val yStr = year.toString().padStart(4, '0')
        val mStr = m.toString().padStart(2, '0')
        val dStr = d.toString().padStart(2, '0')
        val hStr = hours.toString().padStart(2, '0')
        val minStr = minutes.toString().padStart(2, '0')
        val sStr = secs.toString().padStart(2, '0')
        val msStr = millis.coerceAtLeast(0).toString().padStart(3, '0')

        return "${yStr}-${mStr}-${dStr}T${hStr}:${minStr}:${sStr}.${msStr}Z"
    }

    fun parseIsoToEpoch(iso: String): Long {
        return try {
            val clean = iso.trim().removeSuffix("Z")
            val parts = clean.split("T")
            if (parts.size != 2) return PlatformEnvironment.currentTimeMillis() + 60_000L
            val dateParts = parts[0].split("-").map { it.toInt() }
            val year = dateParts[0]
            val month = dateParts[1]
            val day = dateParts[2]

            val timeParts = parts[1].split(":")
            val hour = timeParts[0].toInt()
            val min = timeParts[1].toInt()
            val secParts = timeParts[2].split(".")
            val sec = secParts[0].toInt()
            val millis = if (secParts.size > 1) secParts[1].take(3).padEnd(3, '0').toInt() else 0

            val y = year - (if (month <= 2) 1 else 0)
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val m = month + (if (month > 2) -3 else 9)
            val doy = (153 * m + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            val days = era * 146097 + doe - 719468

            val totalSec = days * 86400L + hour * 3600L + min * 60L + sec
            totalSec * 1000L + millis
        } catch (_: Exception) {
            PlatformEnvironment.currentTimeMillis() + 60_000L
        }
    }
}
