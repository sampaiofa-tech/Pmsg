package com.example.ai

actual class GeminiService {
    actual suspend fun generateEphemeralBurnerNote(prompt: String): String {
        return "Nota efêmera Web: $prompt"
    }
}
