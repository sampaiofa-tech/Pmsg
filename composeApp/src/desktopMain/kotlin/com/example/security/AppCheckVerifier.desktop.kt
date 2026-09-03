package com.example.security

actual object AppCheckVerifier {
    actual suspend fun getAttestationToken(): String? {
        // Desktop JVM: Não possui SDK oficial do Firebase App Check.
        // A validação de integridade e acesso a recursos protegidos é feita
        // através de token de sessão próprio assinado e validado pelo Proxy Backend (geminiProxy).
        return DesktopSessionManager.getActiveSessionToken()
    }
}

object DesktopSessionManager {
    private var cachedToken: String? = null

    fun getActiveSessionToken(): String {
        return cachedToken ?: "PMSG_DESKTOP_SESSION_${System.currentTimeMillis()}".also {
            cachedToken = it
        }
    }
}
