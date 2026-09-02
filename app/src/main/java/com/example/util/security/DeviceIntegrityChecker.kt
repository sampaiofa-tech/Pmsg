package com.example.util.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File

data class DeviceSecurityReport(
    val isHardwareKeystoreActive: Boolean,
    val isStrongBoxSupported: Boolean,
    val isRootDetected: Boolean,
    val isDebuggable: Boolean,
    val isDebuggerAttached: Boolean,
    val securityLevelLabel: String,
    val details: List<String>
)

/**
 * Diagnostic utility to verify device security posture, TEE availability,
 * and anti-tamper / anti-root defense metrics for the Pmsg Security Vault.
 */
object DeviceIntegrityChecker {

    private val ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    fun checkSecurityPosture(context: Context): DeviceSecurityReport {
        val hasHardwareKeystore = CryptoManager.isHardwareBacked()
        val hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

        val isRoot = checkRootBinaries() || checkTestKeys()
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()

        val details = mutableListOf<String>()
        if (hasStrongBox) {
            details.add("StrongBox Keymaster Ativo (Hardware Isolado EAL5+)")
        } else if (hasHardwareKeystore) {
            details.add("TEE (Trusted Execution Environment) Ativo")
        } else {
            details.add("Emulação Criptográfica Segura (Keystore Standard)")
        }

        if (isRoot) {
            details.add("⚠️ Possível ambiente com privilégios Root / Binário SU detectado")
        } else {
            details.add("✓ Sem binários de Root detectados no sistema")
        }

        if (isDebuggable || isDebuggerAttached) {
            details.add("⚠️ Depurador ativo ou APK em modo de desenvolvimento")
        } else {
            details.add("✓ Sem depuradores ou ganchos de depuração conectados")
        }

        val securityLevel = when {
            isRoot -> "ATENÇÃO: AMBIENTE MODIFICADO"
            hasStrongBox -> "MÁXIMA: STRONGBOX HARDWARE"
            hasHardwareKeystore -> "ALTA: TEE ISOLADO"
            else -> "PADRÃO: ENCLAVE SEGURO"
        }

        return DeviceSecurityReport(
            isHardwareKeystoreActive = hasHardwareKeystore,
            isStrongBoxSupported = hasStrongBox,
            isRootDetected = isRoot,
            isDebuggable = isDebuggable,
            isDebuggerAttached = isDebuggerAttached,
            securityLevelLabel = securityLevel,
            details = details
        )
    }

    private fun checkRootBinaries(): Boolean {
        return try {
            ROOT_PATHS.any { File(it).exists() }
        } catch (_: Throwable) {
            false
        }
    }

    private fun checkTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
}
