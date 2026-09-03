package com.example.data.network

actual object PlatformEnvironment {
    // In production releases, run with -Dpmsg.debug=false or packaged native distribution
    actual val isDebug: Boolean = System.getProperty("pmsg.debug", "true") == "true"

    // ANTI-SPOOFING: Environment variables are strictly ignored in release mode
    actual fun getEnv(name: String): String? {
        return if (isDebug) {
            System.getenv(name)
        } else {
            null
        }
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
