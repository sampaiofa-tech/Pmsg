package com.example.ai

import com.example.data.network.ApiClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

actual class GeminiService {
    actual suspend fun generateEphemeralBurnerNote(prompt: String): String {
        return try {
            val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
            if (apiKey.isBlank()) {
                return "Nota efêmera local Desktop: $prompt"
            }

            val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val bodyPayload = buildJsonObject {
                putJsonArray("contents") {
                    add(buildJsonObject {
                        putJsonArray("parts") {
                            add(buildJsonObject {
                                put("text", "Resuma como nota efêmera privada e concisa em português: $prompt")
                            })
                        }
                    })
                }
            }

            val response = ApiClient.client.post(requestUrl) {
                contentType(ContentType.Application.Json)
                setBody(bodyPayload.toString())
            }

            val responseText = response.bodyAsText()
            val parsed = Json.parseToJsonElement(responseText)
            val text = parsed.jsonObject["candidates"]?.jsonArray?.getOrNull(0)
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.getOrNull(0)
                ?.jsonObject?.get("text")?.jsonPrimitive?.content

            text ?: "Nota efêmera processada"
        } catch (_: Exception) {
            "Nota efêmera local: $prompt"
        }
    }
}
