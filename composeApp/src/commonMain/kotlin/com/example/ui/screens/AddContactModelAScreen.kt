package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepository
import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.IdentityManager
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun AddContactModelAScreen(
    contactRepository: ContactRepository,
    onBack: () -> Unit,
    onContactCreated: (ContactItem) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    // My URI for sharing
    var myUri by remember { mutableStateOf("") }
    var myFingerprint by remember { mutableStateOf("") }
    var copyFeedback by remember { mutableStateOf<String?>(null) }

    // Input fields for pasting code
    var inputName by remember { mutableStateOf("") }
    var inputUri by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val identity = IdentityManager.getOrGenerateIdentity()
        myFingerprint = identity.fingerprintHex
        myUri = IdentityManager.createContactUri("user_${identity.fingerprintHex.take(8)}") ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Troca Presencial (Modelo A)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs: [Meu Código (Compartilhar), Adicionar Contato (Colar)]
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0D1B2A),
                contentColor = Color(0xFF00FFC2),
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00FFC2)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Meu Código", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Colar Código", fontWeight = FontWeight.Bold) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedTab == 0) {
                    // TAB 0: Share My Code
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Compartilhe seu Código com o Contato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No celular escaneie o QR Code; no Desktop copie o código URI abaixo e envie ao contato presencialmente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // URI Code Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2A))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "STRING-CÓDIGO (MODELO A):",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00FFC2),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = myUri,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFE0E0E0),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(myUri))
                                    copyFeedback = "Código copiado para a área de transferência!"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color(0xFF0A1128),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Copiar Código",
                                        color = Color(0xFF0A1128),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    copyFeedback?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = Color(0xFF00FFC2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                } else {
                    // TAB 1: Paste Other's Code
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF131B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color(0xFF00FFC2),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Adicionar Novo Contato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cole o código `pmsg://contact` fornecido pelo seu contato. A chave pública será validada criptograficamente contra o fingerprint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Name Input
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Nome do Contato (ex: Alice)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFC2),
                            unfocusedBorderColor = Color(0xFF2E3D52),
                            focusedContainerColor = Color(0xFF131B2A),
                            unfocusedContainerColor = Color(0xFF131B2A)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // URI Code Input
                    OutlinedTextField(
                        value = inputUri,
                        onValueChange = {
                            inputUri = it
                            errorMessage = null
                        },
                        label = { Text("Cole o Código (pmsg://contact?...)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFC2),
                            unfocusedBorderColor = Color(0xFF2E3D52),
                            focusedContainerColor = Color(0xFF131B2A),
                            unfocusedContainerColor = Color(0xFF131B2A)
                        ),
                        maxLines = 4
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (inputName.isBlank()) {
                                errorMessage = "Por favor, digite um nome para o contato."
                                return@Button
                            }
                            if (inputUri.isBlank()) {
                                errorMessage = "Por favor, cole a string-código do contato."
                                return@Button
                            }

                            val parseResult = IdentityManager.parseContactUri(inputUri.trim())
                            if (parseResult.isFailure) {
                                errorMessage = parseResult.exceptionOrNull()?.message
                                    ?: "Código inválido ou corrompido."
                                return@Button
                            }

                            val contactData = parseResult.getOrThrow()
                            val myIdentity = IdentityManager.getOrGenerateIdentity()

                            // Compute commutative Pair Safety Number
                            val pairSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
                                myPubKey = myIdentity.publicKey,
                                peerPubKey = contactData.publicKeyBytes
                            )

                            val newContact = ContactItem(
                                fingerprint = contactData.fingerprintHex,
                                pubKey = contactData.publicKeyBase64,
                                currentAuthUid = contactData.authUid,
                                displayName = inputName.trim(),
                                securityNumber = pairSafetyNumber,
                                verified = false,
                                addedAt = System.currentTimeMillis()
                            )

                            coroutineScope.launch {
                                contactRepository.saveContact(newContact)
                                onContactCreated(newContact)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFC2))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF0A1128)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Validar e Comparar Código (60 Dígitos)",
                                color = Color(0xFF0A1128),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
