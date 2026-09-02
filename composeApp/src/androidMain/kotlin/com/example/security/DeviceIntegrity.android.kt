package com.example.security

import android.content.Context
import com.example.util.security.DeviceIntegrityChecker

actual object DeviceIntegrity {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun checkIntegrity(): IntegrityResult {
        val ctx = appContext
        if (ctx == null) {
            return IntegrityResult(isCompromised = false, summary = "Verificação pendente de contexto")
        }

        val report = DeviceIntegrityChecker.checkSecurityPosture(ctx)
        return IntegrityResult(
            isCompromised = !report.isDeviceSecure,
            isEmulator = !report.isDeviceSecure && !report.isRootDetected,
            isDebuggerAttached = report.isDebuggerAttached,
            summary = if (!report.isDeviceSecure) "Ameaças detectadas: ${report.details.joinToString()}" else "Ambiente seguro"
        )
    }
}
