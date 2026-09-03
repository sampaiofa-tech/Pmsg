package com.example.data.network

actual object PlatformEnvironment {
    actual val isDebug: Boolean = false
    actual fun getEnv(name: String): String? = null
    actual fun currentTimeMillis(): Long = (platform.Foundation.NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
