package com.example.ai

import com.example.data.network.ApiClient
import com.example.security.AppCheckVerifier
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

actual class GeminiService {
    // Zero-Trace Architecture: A chave GEMINI_API_KEY reside EXCLUSIVAMENTE server-side
    // no Secret Manager do Firebase / Cloud Functions.
    // O cliente Desktop autentica-se com o token de sessão e invoca o endpoint proxy seguro.
    private val proxyEndpoint: String
        get() = com.example.data.network.AppEndpoints.geminiProxyUrl

    actual suspend fun generateEphemeralBurnerNote(prompt: String): String {
        return try {
            val sessionToken = AppCheckVerifier.getAttestationToken() ?: ""
            val bodyPayload = buildJsonObject {
                put("prompt", prompt)
                put("model", "gemini-2.0-flash")
            }

            val response = ApiClient.client.post(proxyEndpoint) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $sessionToken")
                setBody(bodyPayload.toString())
            }

            val responseText = response.bodyAsText()
            val parsed = Json.parseToJsonElement(responseText)
            val note = parsed.jsonObject["note"]?.jsonPrimitive?.content

            note ?: "Nota efêmera local: $prompt"
        } catch (_: Exception) {
            "Nota efêmera local Desktop: $prompt"
        }
    }
}
