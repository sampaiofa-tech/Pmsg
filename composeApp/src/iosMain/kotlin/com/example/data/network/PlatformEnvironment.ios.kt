package com.example.data.network

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object PlatformEnvironment {
    actual val isDebug: Boolean = false
    actual fun getEnv(name: String): String? = null
    actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
