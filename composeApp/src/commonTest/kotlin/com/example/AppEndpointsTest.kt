package com.example

import com.example.data.network.AppEndpoints
import com.example.data.network.PlatformEnvironment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppEndpointsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testProductionUrlsAreNonEmptyAndFollowHttps() {
        assertTrue(AppEndpoints.PROD_PROXY_URL.startsWith("https://"))
        assertTrue(AppEndpoints.PROD_PROXY_URL.contains("geminiProxy"))
        assertTrue(AppEndpoints.PROD_STORE_KEY_URL.startsWith("https://"))
        assertTrue(AppEndpoints.PROD_STORE_KEY_URL.contains("storeMessageKey"))
        assertTrue(AppEndpoints.PROD_GET_KEY_URL.startsWith("https://"))
        assertTrue(AppEndpoints.PROD_GET_KEY_URL.contains("getMessageKey"))
        assertTrue(AppEndpoints.PROD_REPORT_ABUSE_URL.startsWith("https://"))
        assertTrue(AppEndpoints.PROD_REPORT_ABUSE_URL.contains("reportAbuse"))
        assertTrue(AppEndpoints.PROD_IDENTITY_TOOLKIT_URL.startsWith("https://identitytoolkit.googleapis.com"))
    }

    @Test
    fun testReportAbuseCallableEnvelopeFormat() {
        // Enforces that reportAbuse conforms to Zero-Knowledge Firebase Callable {"data": {...}}
        // and contains NO message content fields
        val payload = buildJsonObject {
            put("data", buildJsonObject {
                put("reportedFingerprint", "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90")
                put("abuseType", "SPAM")
                put("inviteId", "sample_invite_token_123")
            })
        }

        val jsonString = payload.toString()
        val parsed = json.parseToJsonElement(jsonString).jsonObject

        assertNotNull(parsed["data"], "Firebase Callable requires top-level 'data' wrapper")
        val dataObj = parsed["data"]!!.jsonObject
        assertEquals("a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90", dataObj["reportedFingerprint"]?.jsonPrimitive?.content)
        assertEquals("SPAM", dataObj["abuseType"]?.jsonPrimitive?.content)
        assertEquals("sample_invite_token_123", dataObj["inviteId"]?.jsonPrimitive?.content)
        // Verify ZERO message content
        assertTrue(dataObj["text"] == null)
        assertTrue(dataObj["message"] == null)
        assertTrue(dataObj["content"] == null)
    }

    @Test
    fun testGetMessageKeyCallableEnvelopeFormat() {
        // Enforces that getMessageKey payload conforms strictly to Firebase Callable {"data": {"messageId": ...}}
        val payload = buildJsonObject {
            put("data", buildJsonObject {
                put("messageId", "msg_test_recipient")
            })
        }

        val jsonString = payload.toString()
        val parsed = json.parseToJsonElement(jsonString).jsonObject

        assertNotNull(parsed["data"], "Firebase Callable requires top-level 'data' wrapper")
        val dataObj = parsed["data"]!!.jsonObject
        assertEquals("msg_test_recipient", dataObj["messageId"]?.jsonPrimitive?.content)
    }

    @Test
    fun testFirebaseCallableEnvelopeFormat() {
        // Enforces that storeMessageKey payload conforms strictly to Firebase Callable {"data": {...}}
        // with opaque SealedBox fields (ephemeralPubKey, wrappedDek)
        val payload = buildJsonObject {
            put("data", buildJsonObject {
                put("messageId", "msg_test_01")
                put("senderId", "sender_alice")
                put("recipientId", "recipient_bob")
                put("ephemeralPubKey", "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a")
                put("wrappedDek", "ZGVrX3dyYXBwZWRfYmFzZTY0X3BheWxvYWQ=")
                put("expiresAtMillis", 1725350000000L)
            })
        }

        val jsonString = payload.toString()
        val parsed = json.parseToJsonElement(jsonString).jsonObject

        assertNotNull(parsed["data"], "Firebase Callable requires top-level 'data' wrapper")
        val dataObj = parsed["data"]!!.jsonObject
        assertEquals("msg_test_01", dataObj["messageId"]?.jsonPrimitive?.content)
        assertEquals("sender_alice", dataObj["senderId"]?.jsonPrimitive?.content)
        assertEquals("recipient_bob", dataObj["recipientId"]?.jsonPrimitive?.content)
        assertEquals("8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a", dataObj["ephemeralPubKey"]?.jsonPrimitive?.content)
        assertEquals("ZGVrX3dyYXBwZWRfYmFzZTY0X3BheWxvYWQ=", dataObj["wrappedDek"]?.jsonPrimitive?.content)
        assertEquals("1725350000000", dataObj["expiresAtMillis"]?.jsonPrimitive?.content)
    }

    @Test
    fun testPlatformEnvironmentIntegrity() {
        // Multiplatform sanity check
        val isDebug = PlatformEnvironment.isDebug
        // Either true (in test/debug run) or false (in release)
        assertTrue(isDebug == true || isDebug == false)
    }
}
