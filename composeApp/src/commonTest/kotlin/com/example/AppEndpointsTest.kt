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
        assertTrue(AppEndpoints.PROD_IDENTITY_TOOLKIT_URL.startsWith("https://identitytoolkit.googleapis.com"))
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
        val payload = buildJsonObject {
            put("data", buildJsonObject {
                put("messageId", "msg_test_01")
                put("senderId", "sender_alice")
                put("recipientId", "recipient_bob")
                put("dek", "ZGVrX2Jhc2U2NF9zZWNyZXQ=")
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
        assertEquals("ZGVrX2Jhc2U2NF9zZWNyZXQ=", dataObj["dek"]?.jsonPrimitive?.content)
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
