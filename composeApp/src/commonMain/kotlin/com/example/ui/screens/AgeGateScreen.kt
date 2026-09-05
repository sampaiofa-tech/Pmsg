package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.consent.LegalConsentManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeGateScreen(
    onConsentAccepted: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isAge18Confirmed by remember { mutableStateOf(false) }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var showRefusalDialog by remember { mutableStateOf(false) }

    val isFormValid = isAge18Confirmed && isTermsAccepted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Raix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Classificação 18+ & Termos de Serviço",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD4AF37)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1325),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1325))
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Badge de Restrição Etária
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131F37))
                    .border(2.dp, Color(0xFF00E676), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "18+",
                    color = Color(0xFF00E676),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Classificação Indicativa Estrita",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "O Raix é um mensageiro efêmero de comunicação privada entre adultos. Em conformidade com a legislação brasileira (ECA/LGPD) e as políticas de segurança de conteúdo, o uso por menores de 18 anos é expressamente vedado.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card informativo de Arquitetura e Regras
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111927)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Condições Fundamentais do Serviço",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Criptografia ponta-a-ponta (E2EE) sealed-box: o servidor é matematicamente cego e não possui as chaves para decifrar mensagens.\n• Autodestruição programada: mensagens expiram e são destruídas sem histórico persistente em servidores.\n• Responsabilidade individual: a custódia das chaves e do mnemônico de 12 palavras é exclusiva do usuário.\n• Condutas proibidas: assédio, spam e compartilhamento de materiais ilícitos sujeitam o infrator a bloqueio e denúncia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90A4AE),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Links Legais Públicos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = { uriHandler.openUri(LegalConstants.TERMS_URL) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF00FFC2)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Termos de Uso",
                        color = Color(0xFF00FFC2),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TextButton(
                    onClick = { uriHandler.openUri(LegalConstants.POLICY_URL) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF00FFC2)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Política de Privacidade",
                        color = Color(0xFF00FFC2),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFF1E293B)
            )

            // Checkbox 1: Idade 18+
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isAge18Confirmed = !isAge18Confirmed }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAge18Confirmed,
                    onCheckedChange = { isAge18Confirmed = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00FFC2),
                        checkmarkColor = Color(0xFF0A1128),
                        uncheckedColor = Color(0xFF64748B)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Declaro sob as penas da lei ter 18 anos completos ou mais.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAge18Confirmed) Color.White else Color(0xFFB0BEC5),
                    fontWeight = if (isAge18Confirmed) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // Checkbox 2: Termos de Uso e Política
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isTermsAccepted = !isTermsAccepted }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isTermsAccepted,
                    onCheckedChange = { isTermsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00FFC2),
                        checkmarkColor = Color(0xFF0A1128),
                        uncheckedColor = Color(0xFF64748B)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Li, compreendi e concordo integralmente com os Termos de Uso e com a Política de Privacidade (v${LegalConsentManager.CURRENT_LEGAL_VERSION}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isTermsAccepted) Color.White else Color(0xFFB0BEC5),
                    fontWeight = if (isTermsAccepted) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Botão Principal: Entrar no Raix
            Button(
                onClick = {
                    if (isFormValid) {
                        LegalConsentManager.recordConsent(confirmedAge18 = true)
                        onConsentAccepted()
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF0B1325),
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                Text(
                    text = "Entrar no Raix",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Secundário: Recusar
            TextButton(
                onClick = { showRefusalDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Não aceito / Tenho menos de 18 anos",
                    color = Color(0xFFFF8080),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRefusalDialog) {
        AlertDialog(
            onDismissRequest = { showRefusalDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Acesso Bloqueado", color = Color.White)
                }
            },
            text = {
                Text(
                    text = "Sem a confirmação de maioridade (18+) e o aceite formal dos Termos de Uso e da Política de Privacidade, o acesso às funcionalidades do Raix permanece terminantemente bloqueado.\n\nVocê pode fechar o aplicativo ou retornar para aceitar quando atingir a maioridade legal.",
                    color = Color(0xFFCFD8DC),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRefusalDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                ) {
                    Text(text = "Entendi", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E222A)
        )
    }
}
