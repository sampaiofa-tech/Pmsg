package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.IdentityNetworkClient
import com.example.data.network.PlatformEnvironment
import com.example.security.BiometricAuth
import com.example.security.ClipboardSensivel
import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.IdentityKeyPair
import com.example.security.identity.IdentityManager
import com.example.security.identity.ProvisionedIdentity
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalEncodingApi::class)
@Composable
fun IdentityScreen(
    currentAuthUid: String = "anonymous_uid",
    onBack: () -> Unit = {},
    onProvisioned: () -> Unit = {},
    onOpenDataPrivacy: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = remember { ClipboardSensivel() }
    val biometricAuth = remember { BiometricAuth() }

    var identity by remember { mutableStateOf<IdentityKeyPair?>(IdentityManager.getIdentity()) }
    var showProvisioningDialog by remember { mutableStateOf(!IdentityManager.hasIdentity()) }
    var provisionedDraft by remember { mutableStateOf<ProvisionedIdentity?>(null) }

    // Mnemonic viewing security state
    var showMnemonicWarningDialog by remember { mutableStateOf(false) }
    var isMnemonicUnlocked by remember { mutableStateOf(false) }
    var mnemonicWords by remember { mutableStateOf<List<String>?>(null) }

    // Recovery & Restoration state
    var showRestoreDialog by remember { mutableStateOf(false) }
    var inputRestoreMnemonic by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // Auto-generate draft if identity does not exist
    LaunchedEffect(showProvisioningDialog) {
        if (showProvisioningDialog && provisionedDraft == null) {
            provisionedDraft = IdentityManager.provisionNewIdentity()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF00FFC2))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Minha Identidade", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDataPrivacy) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Sobre seus dados (LGPD)",
                            tint = Color(0xFF00FFC2)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Security Badge
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E1B)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00FFC2).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00FFC2))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Identidade Criptográfica X25519",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF00FFC2),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Protegida em repouso por Hardware KeyVault • Padrão Signal • Zero Rastro",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Fingerprint (60 Digits) Card
            if (identity != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Número de Segurança (Fingerprint)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    identity?.safetyNumber?.let { sn ->
                                        clipboard.copySensitive(sn, "Número de Segurança")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Número de segurança copiado (auto-limpeza em 30s) 🔒")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color(0xFF00FFC2))
                            }
                        }

                        Text(
                            "Estes 60 dígitos representam o hash criptográfico exclusivo da sua chave pública X25519.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Render 12 blocks of 5 digits in clean grouped blocks
                        val blocks = identity?.safetyNumber?.split(" ") ?: emptyList()
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            blocks.forEach { block ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E2825))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = block,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF00FFC2)
                                    )
                                }
                            }
                        }
                    }
                }

                // Presencial Exchange Card (Model A)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF00FFC2))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Troca Presencial (Modelo A)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Use o código abaixo para que seus contatos adicionem você diretamente sem consultar servidores ou diretórios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val uri = IdentityManager.createContactUri(currentAuthUid) ?: ""

                        OutlinedTextField(
                            value = uri,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Código de Contato Presencial") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        clipboard.copySensitive(uri, "Código de Contato")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Código presencial copiado com sucesso! 📋")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Código")
                                }
                            }
                        )
                    }
                }

                // Mnemonic Recovery Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFFFB300))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Mnemônico de Recuperação (BIP-39)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "As 12 palavras em português são a única forma de restaurar sua identidade caso perca este dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isMnemonicUnlocked) {
                            Button(
                                onClick = { showMnemonicWarningDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3A24)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFFFFB300))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver Mnemônico de 12 Palavras", color = Color(0xFFFFB300))
                            }
                        } else {
                            val words = mnemonicWords ?: emptyList()
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                words.forEachIndexed { index, word ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF252F2B))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}. $word",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            color = Color(0xFFFFE082)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val phrase = words.joinToString(" ")
                                        clipboard.copySensitive(phrase, "Mnemônico Pmsg")
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Mnemônico copiado (auto-destruição em 30s) 🔒")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copiar Frase")
                                }

                                Button(
                                    onClick = {
                                        isMnemonicUnlocked = false
                                        mnemonicWords = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ocultar")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                inputRestoreMnemonic = ""
                                restoreError = null
                                showRestoreDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A3A))
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF00FFC2), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar Outro Aparelho (Recovery)", color = Color(0xFF00FFC2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Not provisioned fallback
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.padding(bottom = 12.dp))
                        Text(
                            "Nenhuma Identidade Configurada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Você precisa gerar uma identidade criptográfica para usar o Pmsg com segurança máxima.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                provisionedDraft = IdentityManager.provisionNewIdentity()
                                showProvisioningDialog = true
                            }
                        ) {
                            Text("Criar Minha Identidade")
                        }
                    }
                }
            }

            // Seção de Transparência e Dados (LGPD)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                    .clickable { onOpenDataPrivacy() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF00FFC2),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sobre seus dados",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inventário em linguagem simples, conformidade LGPD e canal do Encarregado",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF00FFC2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Warning Dialog before showing mnemonic
    if (showMnemonicWarningDialog) {
        AlertDialog(
            onDismissRequest = { showMnemonicWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300)) },
            title = { Text("Aviso de Segurança") },
            text = {
                Text("Certifique-se de que está em um local privativo e que ninguém está observando sua tela. Suas 12 palavras dão acesso irrestrito à sua identidade.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMnemonicWarningDialog = false
                        coroutineScope.launch {
                            val authenticated = biometricAuth.authenticate(
                                title = "Revisão de Mnemônico",
                                subtitle = "Confirme sua biometria ou PIN para visualizar o mnemônico de 12 palavras"
                            )
                            if (authenticated) {
                                mnemonicWords = IdentityManager.getMnemonicWords()
                                isMnemonicUnlocked = true
                            } else {
                                snackbarHostState.showSnackbar("Autenticação necessária para revelar o mnemônico 🔒")
                            }
                        }
                    }
                ) {
                    Text("Exibir Palavras com Biometria")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMnemonicWarningDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Initial Provisioning Dialog with 3-word random confirmation
    if (showProvisioningDialog && provisionedDraft != null) {
        val draft = provisionedDraft!!
        var step by remember { mutableStateOf(1) } // 1: show words, 2: test 3 words

        // Pick 3 distinct random indices for verification (1-based: 1..12)
        val testIndices = remember(draft) {
            val list = (1..12).shuffled(Random(draft.mnemonic.hashCode())).take(3).sorted()
            list
        }

        var inputWord1 by remember { mutableStateOf("") }
        var inputWord2 by remember { mutableStateOf("") }
        var inputWord3 by remember { mutableStateOf("") }
        var verificationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { /* Modal: cannot dismiss without completing or canceling */ },
            title = {
                Text(if (step == 1) "🔒 Provisionamento de Identidade" else "Verificação do Mnemônico")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step == 1) {
                        Text(
                            "Anote as 12 palavras abaixo na ordem correta em um papel e guarde-o em local estritamente seguro.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141E1B))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            draft.mnemonic.forEachIndexed { index, word ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF22312C))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}. $word",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FFC2)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "Para confirmar que você anotou, informe as 3 palavras solicitadas abaixo:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = inputWord1,
                            onValueChange = { inputWord1 = it.trim().lowercase() },
                            label = { Text("Palavra #${testIndices[0]}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputWord2,
                            onValueChange = { inputWord2 = it.trim().lowercase() },
                            label = { Text("Palavra #${testIndices[1]}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = inputWord3,
                            onValueChange = { inputWord3 = it.trim().lowercase() },
                            label = { Text("Palavra #${testIndices[2]}") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (verificationError != null) {
                            Text(
                                text = verificationError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (step == 1) {
                    Button(onClick = { step = 2 }) {
                        Text("Já Anotei, Continuar")
                    }
                } else {
                    Button(
                        onClick = {
                            val expected1 = draft.mnemonic[testIndices[0] - 1]
                            val expected2 = draft.mnemonic[testIndices[1] - 1]
                            val expected3 = draft.mnemonic[testIndices[2] - 1]

                            if (inputWord1 == expected1 && inputWord2 == expected2 && inputWord3 == expected3) {
                                IdentityManager.confirmAndSaveIdentity(draft)
                                identity = draft.keyPair
                                showProvisioningDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Identidade provisionada com sucesso! 🛡️")
                                }
                                onProvisioned()
                            } else {
                                verificationError = "Palavras incorretas. Verifique suas anotações."
                            }
                        }
                    ) {
                        Text("Confirmar e Salvar")
                    }
                }
            },
            dismissButton = {
                if (step == 2) {
                    TextButton(onClick = { step = 1; verificationError = null }) {
                        Text("Voltar e Rever")
                    }
                } else {
                    TextButton(onClick = {
                        showProvisioningDialog = false
                        inputRestoreMnemonic = ""
                        restoreError = null
                        showRestoreDialog = true
                    }) {
                        Text("Já possuo 12 palavras (Restaurar)", color = Color(0xFF00FFC2), fontSize = 12.sp)
                    }
                }
            }
        )
    }

    // Restore from Mnemonic (Recovery) Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF00FFC2))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar Identidade (Recovery)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Informe as 12 palavras do seu mnemônico BIP-39 em português para restaurar sua chave privada X25519 e o mesmo Número de Segurança.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ DESIGN EFÊMERO: Mensagens de até 24h destinadas à sessão do aparelho anterior foram destruídas por design (sem histórico permanente nos servidores).",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputRestoreMnemonic,
                        onValueChange = { inputRestoreMnemonic = it },
                        label = { Text("12 Palavras em Português") },
                        placeholder = { Text("palavra1 palavra2 ... palavra12") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FFC2),
                            unfocusedBorderColor = Color(0xFF2A3B4D)
                        )
                    )

                    if (restoreError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = restoreError ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val words = inputRestoreMnemonic.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
                        if (words.size != 12) {
                            restoreError = "O mnemônico deve conter exatamente 12 palavras (fornecidas: ${words.size})."
                            return@Button
                        }
                        isRestoring = true
                        restoreError = null
                        coroutineScope.launch {
                            val result = IdentityManager.restoreFromMnemonic(words)
                            if (result.isFailure) {
                                isRestoring = false
                                restoreError = "Mnemônico inválido ou checksum incorreto: ${result.exceptionOrNull()?.message}"
                                return@launch
                            }
                            val keyPair = result.getOrThrow()
                            identity = keyPair

                            // Call updateIdentityRouting Cloud Function with Ed25519 Proof-of-Possession (F0)
                            val pubKeyB64 = Base64.encode(keyPair.publicKey)
                            val signingPubKeyB64 = Base64.encode(keyPair.signingPublicKey)
                            val timestamp = PlatformEnvironment.currentTimeMillis()
                            val signatureBytes = IdentityCryptoManager.signRoutingUpdate(
                                signingPrivKeySeed = keyPair.signingPrivateKey,
                                fingerprint = keyPair.fingerprintHex,
                                newAuthUid = currentAuthUid,
                                timestamp = timestamp
                            )
                            val signatureB64 = Base64.encode(signatureBytes)

                            val updateResult = IdentityNetworkClient.updateIdentityRouting(
                                fingerprint = keyPair.fingerprintHex,
                                pubKey = pubKeyB64,
                                signature = signatureB64,
                                timestamp = timestamp,
                                idToken = "anonymous_token",
                                signingPubKey = signingPubKeyB64
                            )

                            if (updateResult.isFailure) {
                                isRestoring = false
                                restoreError = "Falha ao vincular roteamento (prova de posse): ${updateResult.exceptionOrNull()?.message}"
                                return@launch
                            }

                            isRestoring = false
                            showRestoreDialog = false
                            snackbarHostState.showSnackbar("Identidade restaurada com sucesso! Roteamento técnico atualizado 🛡️")
                            onProvisioned()
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                ) {
                    Text(
                        text = if (isRestoring) "Restaurando..." else "Restaurar e Vincular Roteamento",
                        color = Color(0xFF0A1128),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreDialog = false },
                    enabled = !isRestoring
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
