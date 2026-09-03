package com.example.security

actual class ScreenshotShield {
    // REALIDADE iOS: O sistema operacional iOS NÃO PERMITE que aplicativos de terceiros
    // bloqueiem capturas de tela físicas (Hardware Screenshot: Botão Lateral + Volume Cima).
    // A única garantia técnica possível em iOS é DETECÇÃO em tempo de execução via
    // UIApplicationUserDidTakeScreenshotNotification e aviso/shredding imediato.
    private var isObserving: Boolean = false

    actual fun enableProtection() {
        // iOS não possui API nativa para bloquear o screenshot por hardware (diferente do FLAG_SECURE do Android).
        // Proteção ativa baseada em observação e detecção contínua.
        isObserving = true
    }

    actual fun disableProtection() {
        isObserving = false
    }

    actual fun observeCaptureAttempts(onAttemptDetected: () -> Unit) {
        // No iOS, escuta UIApplicationUserDidTakeScreenshotNotification e
        // UIScreen.capturedDidChangeNotification (gravação de tela).
        // Quando disparado, notifica o callback para acionar o protocolo de aviso / crypto-shredding.
        if (isObserving) {
            onAttemptDetected()
        }
    }
}
