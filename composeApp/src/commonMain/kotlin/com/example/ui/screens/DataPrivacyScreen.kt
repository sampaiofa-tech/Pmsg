package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.ClipboardSensivel
import com.example.security.identity.IdentityManager
import kotlinx.coroutines.launch

object LegalConstants {
    const val DPO_NAME = "Filippe Andrade Sampaio"
    const val DPO_EMAIL = "contato@raixtech.com"
    const val POLICY_URL = "https://raixtech.com/privacidade.html"
    const val TERMS_URL = "https://raixtech.com/termos.html"
    const val SERVER_REGION = "us-central1 (EUA)"
    const val LOG_RETENTION_DAYS = 30
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPrivacyScreen(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val clipboard = remember { ClipboardSensivel() }

    val currentIdentity = remember { IdentityManager.getIdentity() }
    val userFingerprint = currentIdentity?.fingerprintHex ?: "Identidade não provisionada"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sobre seus dados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Transparência & Conformidade LGPD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF80CBC4)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cartão de Destaque: Arquitetura Zero-Knowledge
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FFC2).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF00FFC2),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Arquitetura Zero-Knowledge",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Privacidade por Projeto (Privacy by Design)",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "O Pmsg foi construído de forma que o servidor é matematicamente incapaz de ler o conteúdo das mensagens ou as chaves simétricas de criptografia (DEK). As chaves privadas nunca saem do seu dispositivo.",
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            // Meu Fingerprint Criptográfico
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFF00FFC2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Meu Fingerprint Técnico",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                        if (currentIdentity != null) {
                            IconButton(
                                onClick = {
                                    clipboard.copySensitive(userFingerprint, "Fingerprint")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Fingerprint copiado (auto-limpeza em 30s) 🔒")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar Fingerprint",
                                    tint = Color(0xFF80CBC4),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF0A0E17),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = userFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF00FFC2),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Identificador público derivado da sua chave Ed25519. Usado para roteamento seguro e requisição de exclusão de identidade.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }

            // Seção: O que o servidor VÊ vs NUNCA VÊ
            Text(
                text = "INVENTÁRIO SIMPLIFICADO DE DADOS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF80CBC4),
                letterSpacing = 1.sp
            )

            // Cartão: O que o servidor VÊ
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "O que o Servidor VÊ (Dados Técnicos Mínimos)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD),
                            fontSize = 14.sp
                        )
                    }
                    DataPointRow(
                        title = "UID Anônimo do Firebase Auth",
                        description = "Identificador aleatório sem e-mail, telefone ou nome real.",
                        accent = Color(0xFF60A5FA)
                    )
                    DataPointRow(
                        title = "Timestamps Técnicos",
                        description = "Data/hora de criação e expiração para controle do ciclo de vida efêmero.",
                        accent = Color(0xFF60A5FA)
                    )
                    DataPointRow(
                        title = "Ciphertext Efêmero",
                        description = "Bytes cifrados da mensagem, totalmente ilegíveis sem a chave privada.",
                        accent = Color(0xFF60A5FA)
                    )
                    DataPointRow(
                        title = "DEK Envelopada (bytes opacos)",
                        description = "Chave da mensagem cifrada com X25519; o servidor não tem a chave de abertura.",
                        accent = Color(0xFF60A5FA)
                    )
                    DataPointRow(
                        title = "Chave Efêmera (ephemeralPubKey)",
                        description = "Chave pública por mensagem para permitir o handshake sealed-box do destinatário.",
                        accent = Color(0xFF60A5FA)
                    )
                }
            }

            // Cartão: O que o servidor NUNCA VÊ
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1523)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF4C1D3D), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFF472B6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "O que o Servidor NUNCA VÊ (Exclusivo Local)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF472B6),
                            fontSize = 14.sp
                        )
                    }
                    DataPointRow(
                        title = "Conteúdo Legível das Mensagens",
                        description = "Textos em claro existem unicamente na tela dos participantes.",
                        accent = Color(0xFFF472B6)
                    )
                    DataPointRow(
                        title = "Sua Frase Mnemônica (12 Palavras)",
                        description = "Nunca sai da memória segura do dispositivo.",
                        accent = Color(0xFFF472B6)
                    )
                    DataPointRow(
                        title = "Chaves Privadas (Ed25519 e X25519)",
                        description = "Armazenadas localmente com criptografia de chave em repouso.",
                        accent = Color(0xFFF472B6)
                    )
                    DataPointRow(
                        title = "Seus Contatos e Apelidos",
                        description = "Grafo de contatos reside 100% offline no banco local Room.",
                        accent = Color(0xFFF472B6)
                    )
                }
            }

            // Nota da Câmera Local
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF00FFC2),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Câmera: Processamento 100% Local",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A câmera é utilizada exclusivamente para a leitura presencial de QR codes (troca de contatos e convites). Nenhuma imagem, foto ou vídeo é gravada em disco ou transmitida pela rede.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Infraestrutura, Retenção e Transferência Internacional
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Infraestrutura & Transferência Internacional",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "• Hospedagem: Google Cloud Platform (${LegalConstants.SERVER_REGION})\n" +
                                "• Retenção de logs técnicos: ${LegalConstants.LOG_RETENTION_DAYS} dias (Cloud Logging para auditoria e segurança)\n" +
                                "• Mensagens no servidor: Eliminadas imediatamente após leitura (Vanish) ou no prazo máximo de 24h (TTL)\n" +
                                "• Transferência internacional amparada em certificações globais (ISO/IEC 27001)",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Canal do Encarregado e Direitos LGPD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Canal do Encarregado (DPO) & Direitos LGPD",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Para exercer seus direitos de confirmação, acesso ou eliminação do registro técnico de identidade (doc identities/{fingerprint}), entre em contato com o Encarregado pelo canal:",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.sp
                    )
                    Surface(
                        color = Color(0xFF0A0E17),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LegalConstants.DPO_EMAIL,
                                color = Color(0xFF00FFC2),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            IconButton(
                                onClick = {
                                    clipboard.copySensitive(LegalConstants.DPO_EMAIL, "E-mail do DPO")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("E-mail do DPO copiado!")
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar e-mail",
                                    tint = Color(0xFF80CBC4),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Prazo legal de resposta: até 15 dias (art. 19, II da LGPD). Controlador: ${LegalConstants.DPO_NAME}.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }

            // Links para os Documentos Oficiais no GitHub Pages
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DOCUMENTOS LEGAIS COMPLETOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80CBC4),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LegalLinkRow(
                        title = "Política de Privacidade Integral",
                        subtitle = "Versão 1.0 (Publicada no GitHub Pages)",
                        onClick = {
                            try {
                                uriHandler.openUri(LegalConstants.POLICY_URL)
                            } catch (_: Exception) {
                                clipboard.copySensitive(LegalConstants.POLICY_URL, "Política de Privacidade")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Link copiado para a área de transferência!")
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = Color(0xFF1E3A5F), modifier = Modifier.padding(vertical = 8.dp))
                    LegalLinkRow(
                        title = "Termos de Uso do Serviço",
                        subtitle = "Classificação 18+, Sigilo e Limitações",
                        onClick = {
                            try {
                                uriHandler.openUri(LegalConstants.TERMS_URL)
                            } catch (_: Exception) {
                                clipboard.copySensitive(LegalConstants.TERMS_URL, "Termos de Uso")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Link copiado para a área de transferência!")
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DataPointRow(
    title: String,
    description: String,
    accent: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = description,
                color = Color(0xFF9CA3AF),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LegalLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF80CBC4),
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = Color(0xFF00FFC2),
            modifier = Modifier.size(18.dp)
        )
    }
}
