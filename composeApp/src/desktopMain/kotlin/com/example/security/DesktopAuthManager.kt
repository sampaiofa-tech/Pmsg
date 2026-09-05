package com.example.security

/**
 * Desktop Authentication and Device Identity Manager.
 * Delegates to the unified multiplatform [DeviceAuthManager].
 */
object DesktopAuthManager {

    fun getWebApiKey(): String = com.example.data.network.AppEndpoints.webApiKey

    fun getUserId(): String = DeviceAuthManager.getUserId()

    suspend fun getIdToken(): String? = DeviceAuthManager.getIdToken()

    fun clearSession() = DeviceAuthManager.clearSession()
}
