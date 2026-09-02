package com.example.security

import java.lang.management.ManagementFactory

actual object DeviceIntegrity {
    actual fun checkIntegrity(): IntegrityResult {
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val inputArguments = runtimeMxBean.inputArguments
        val isDebugger = inputArguments.any { it.contains("-agentlib:jdwp") || it.contains("-Xdebug") }

        return IntegrityResult(
            isCompromised = isDebugger,
            isDebuggerAttached = isDebugger,
            summary = if (isDebugger) "Debugger detectado no processo JVM" else "Ambiente Desktop integro"
        )
    }
}
