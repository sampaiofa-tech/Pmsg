package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EphemeralUiMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val isMe: Boolean,
    val text: String,
    val timestamp: Long,
    val ttlMillis: Long,
    val expiresAt: Long,
    var readAt: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactChatScreen(
    contact: ContactItem,
    onBack: () -> Unit,
    onCompareSafetyNumber: () -> Unit,
    onSimulateIncomingReply: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var selectedTtlSeconds by remember { mutableStateOf(60L) } // Default 1 min
    var currentTime by remember { mutableStateOf(0L) }
    var showUnverifiedWarningDialog by remember { mutableStateOf(false) }
    var pendingMessageToSend by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // In-memory ephemeral message queue for this contact session
    val messages = remember {
        mutableStateListOf(
            EphemeralUiMessage(
                id = "welcome_msg",
                senderId = contact.fingerprint,
                senderName = contact.displayName,
                isMe = false,
                text = "Conversa efêmera iniciada com ${contact.displayName}. Criptografada com chaves X25519 locais.",
                timestamp = 0L,
                ttlMillis = 300_000L,
                expiresAt = 300_000L
            )
        )
    }

    // Active real-time countdown timer tick (1 second loop)
    LaunchedEffect(Unit) {
        val startEpoch = System.currentTimeMillis()
        if (messages.isNotEmpty() && messages[0].timestamp == 0L) {
            messages[0] = messages[0].copy(
                timestamp = startEpoch,
                expiresAt = startEpoch + messages[0].ttlMillis
            )
        }
        while (true) {
            currentTime = System.currentTimeMillis()
            // Auto-incinerate expired messages in real time
            messages.removeAll { it.expiresAt <= currentTime }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onCompareSafetyNumber() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (contact.verified) Color(0xFF004D40) else Color(0xFF37474F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.displayName.take(1).uppercase(),
                                color = if (contact.verified) Color(0xFF00FFC2) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = contact.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (contact.verified) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verificado (60 dígitos OK)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF00E676)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Não verificado (Toque para validar)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }
                        }
                    }
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
                actions = {
                    IconButton(
                        onClick = { messages.clear() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Incinerar Conversa Agora",
                            tint = Color(0xFFFF8080)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ChatBottomInputBar(
                text = inputText,
                onTextChanged = { inputText = it },
                selectedTtlSeconds = selectedTtlSeconds,
                onSelectTtlSeconds = { selectedTtlSeconds = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val textToSend = inputText.trim()
                        inputText = ""
                        if (!contact.verified) {
                            pendingMessageToSend = textToSend
                            showUnverifiedWarningDialog = true
                        } else {
                            val now = System.currentTimeMillis()
                            val ttlMs = selectedTtlSeconds * 1000L
                            messages.add(
                                EphemeralUiMessage(
                                    id = "msg_${now}",
                                    senderId = "me",
                                    senderName = "Você",
                                    isMe = true,
                                    text = textToSend,
                                    timestamp = now,
                                    ttlMillis = ttlMs,
                                    expiresAt = now + ttlMs
                                )
                            )
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size)
                                if (onSimulateIncomingReply) {
                                    delay(2500)
                                    val replyNow = System.currentTimeMillis()
                                    messages.add(
                                        EphemeralUiMessage(
                                            id = "reply_${replyNow}",
                                            senderId = contact.fingerprint,
                                            senderName = contact.displayName,
                                            isMe = false,
                                            text = "Resposta segura de ${contact.displayName}: mensagem recebida e chave destruída após decifração.",
                                            timestamp = replyNow,
                                            ttlMillis = ttlMs,
                                            expiresAt = replyNow + ttlMs
                                        )
                                    )
                                    listState.animateScrollToItem(messages.size)
                                }
                            }
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Unverified banner warning
            if (!contact.verified) {
                Surface(
                    color = Color(0xFF332A00),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCompareSafetyNumber() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aviso: Identidade Não Verificada",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Toque aqui para comparar o Número de Segurança de 60 dígitos antes de compartilhar segredos.",
                                color = Color(0xFFFFE082),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    EphemeralMessageBubble(
                        message = msg,
                        currentTime = currentTime,
                        onIncinerate = { messages.remove(msg) }
                    )
                }
            }
        }
    }

    // Unverified warning dialog on send
    if (showUnverifiedWarningDialog && pendingMessageToSend != null) {
        AlertDialog(
            onDismissRequest = {
                showUnverifiedWarningDialog = false
                pendingMessageToSend = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contato Não Verificado")
                }
            },
            text = {
                Text(
                    "Você ainda não comparou o Número de Segurança com ${contact.displayName}.\n\n" +
                    "Recomendamos fazer a verificação presencial do código de 60 dígitos para garantir proteção contra ataques Man-in-the-Middle.\n\n" +
                    "Deseja enviar mesmo assim?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val textToSend = pendingMessageToSend ?: ""
                        showUnverifiedWarningDialog = false
                        pendingMessageToSend = null
                        val now = System.currentTimeMillis()
                        val ttlMs = selectedTtlSeconds * 1000L
                        messages.add(
                            EphemeralUiMessage(
                                id = "msg_${now}",
                                senderId = "me",
                                senderName = "Você",
                                isMe = true,
                                text = textToSend,
                                timestamp = now,
                                ttlMillis = ttlMs,
                                expiresAt = now + ttlMs
                            )
                        )
                        coroutineScope.launch {
                            listState.animateScrollToItem(messages.size)
                        }
                    }
                ) {
                    Text("Enviar Mesmo Assim", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnverifiedWarningDialog = false
                        pendingMessageToSend = null
                        onCompareSafetyNumber()
                    }
                ) {
                    Text("Verificar Agora", color = Color(0xFF00FFC2))
                }
            }
        )
    }
}

@Composable
fun EphemeralMessageBubble(
    message: EphemeralUiMessage,
    currentTime: Long,
    onIncinerate: () -> Unit
) {
    val remainingMs = (message.expiresAt - currentTime).coerceAtLeast(0L)
    val remainingSec = remainingMs / 1000L
    val totalSec = message.ttlMillis / 1000L
    val progress = if (totalSec > 0) (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedRemaining = if (remainingSec >= 3600) {
        val h = remainingSec / 3600
        val m = (remainingSec % 3600) / 60
        val s = remainingSec % 60
        "${h}h ${m.toString().padStart(2, '0')}m"
    } else {
        val m = remainingSec / 60
        val s = remainingSec % 60
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    val bubbleAlignment = if (message.isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMe) Color(0xFF004D40) else Color(0xFF1E293B)
    val accentColor = if (message.isMe) Color(0xFF00FFC2) else Color(0xFF80CBC4)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = bubbleAlignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMe) 16.dp else 4.dp,
                bottomEnd = if (message.isMe) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (message.isMe) "Você" else message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )

                    // Vanish Countdown Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (remainingSec < 15) Color(0xFFFF5252) else Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "💥 $formattedRemaining",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingSec < 15) Color(0xFFFF5252) else Color(0xFFFFD54F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Message text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Visual countdown progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (remainingSec < 15) Color(0xFFFF5252) else Color(0xFF00FFC2),
                    trackColor = Color(0x33000000)
                )
            }
        }
    }
}

@Composable
fun ChatBottomInputBar(
    text: String,
    onTextChanged: (String) -> Unit,
    selectedTtlSeconds: Long,
    onSelectTtlSeconds: (Long) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Color(0xFF0D1B2A),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // TTL Preset Selector Chips: [10s, 1m, 5m, 1h, 24h]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TTL:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF80CBC4),
                    fontWeight = FontWeight.Bold
                )

                val ttlOptions = listOf(
                    10L to "10s",
                    60L to "1m",
                    300L to "5m",
                    3600L to "1h",
                    86400L to "24h"
                )

                ttlOptions.forEach { (seconds, label) ->
                    val isSelected = selectedTtlSeconds == seconds
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTtlSeconds(seconds) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FFC2),
                            selectedLabelColor = Color(0xFF0A1128),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Text input + send button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Mensagem efêmera cifrada...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FFC2),
                        unfocusedBorderColor = Color(0xFF2E3D52),
                        focusedContainerColor = Color(0xFF131B2A),
                        unfocusedContainerColor = Color(0xFF131B2A)
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) Color(0xFF00FFC2) else Color(0xFF263238))
                        .clickable(enabled = text.isNotBlank()) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = if (text.isNotBlank()) Color(0xFF0A1128) else Color(0xFF546E7A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
