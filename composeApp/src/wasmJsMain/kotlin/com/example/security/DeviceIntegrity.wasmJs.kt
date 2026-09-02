package com.example.security

actual object DeviceIntegrity {
    actual fun checkIntegrity(): IntegrityResult {
        // Web context check
        return IntegrityResult(
            isCompromised = false,
            summary = "Navegador Web (Garantia de segurança reduzida — use apps nativos para sigilo máximo)"
        )
    }
}
