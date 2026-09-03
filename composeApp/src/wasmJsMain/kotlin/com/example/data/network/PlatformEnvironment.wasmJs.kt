package com.example.data.network

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual object PlatformEnvironment {
    actual val isDebug: Boolean = false
    actual fun getEnv(name: String): String? = null
    actual fun currentTimeMillis(): Long = jsDateNow().toLong()
}
