package com.example.data.network

actual object PlatformEnvironment {
    actual val isDebug: Boolean = false
    actual fun getEnv(name: String): String? = null
}
