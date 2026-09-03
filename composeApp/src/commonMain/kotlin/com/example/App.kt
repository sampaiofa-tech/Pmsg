package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ai.GeminiService
import kotlinx.coroutines.launch

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiService() }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Pmsg - Zero-Trace Ephemeral Messaging",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                val result = geminiService.generateEphemeralBurnerNote("Gere uma nota de seguranca militar efemera.")
                                aiResponse = result
                                isLoading = false
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            Text("Processando via Proxy...")
                        } else {
                            Text("⚡ Gerar Nota Efemera via Gemini Proxy")
                        }
                    }

                    if (aiResponse != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🤖 Resposta da IA (Producao):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = aiResponse ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
