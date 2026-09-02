package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.BurnerChannel
import com.example.data.model.EphemeralMessage
import com.example.ui.components.AudioRecordingBar
import com.example.ui.components.CallType
import com.example.ui.components.EncryptedCallDialog
import com.example.ui.components.MessageBubble
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveAvatarDeep
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveHeader
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOnlineGreen
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.viewmodel.TtlPreset
import java.util.Locale

data class PendingAttachment(
    val mediaType: String,
    val mediaUri: String,
    val fileName: String,
    val fileSize: String,
    var isViewOnce: Boolean = false,
    var customTtlHours: Float? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channel: BurnerChannel,
    messages: List<EphemeralMessage>,
    currentTime: Long,
    selectedTtl: TtlPreset,
    ttlPresets: List<TtlPreset>,
    userFeedback: String?,
    isScreenshotLockdownActive: Boolean = false,
    vanishAfterReadPresetSeconds: Int = 0,
    shakeDialogVisible: Boolean = false,
    shakeWipeEventTimestamp: Long = 0L,
    onSetVanishAfterReadPresetSeconds: (Int) -> Unit = {},
    onDismissScreenshotLockdown: () -> Unit = {},
    onSimulateScreenshot: () -> Unit = {},
    onSimulateRead: (Long) -> Unit = {},
    onTriggerShakeWipe: () -> Unit = {},
    onDismissShakeDialog: () -> Unit = {},
    onSimulateShake: () -> Unit = {},
    onBack: () -> Unit,
    onSendMessage: (text: String, isBurnerNote: Boolean, isViewOnce: Boolean, customTtlHours: Float?) -> Unit,
    onSendAudio: (durationSeconds: Int, isViewOnce: Boolean, customTtlHours: Float?) -> Unit = { _, _, _ -> },
    onSendMedia: (mediaType: String, mediaUri: String, fileName: String?, fileSize: String?, caption: String, isViewOnce: Boolean, customTtlHours: Float?) -> Unit = { _, _, _, _, _, _, _ -> },
    onSimulateReply: () -> Unit,
    onShredMessage: (Long) -> Unit,
    onIncinerateRoom: (String) -> Unit,
    onSelectTtl: (TtlPreset) -> Unit,
    onClearFeedback: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var isBurnerNoteMode by remember { mutableStateOf(false) }
    var isViewOnceMessage by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var showTtlMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showIncinerateDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<PendingAttachment?>(null) }
    var attachmentCaption by remember { mutableStateOf("") }
    var activeCallType by remember { mutableStateOf<CallType?>(null) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingAttachment = PendingAttachment(
                mediaType = "IMAGE",
                mediaUri = uri.toString(),
                fileName = "Foto_${System.currentTimeMillis().toString().takeLast(4)}.jpg",
                fileSize = "Imagem Criptografada"
            )
        }
    }

    // Android Zero-permission Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingAttachment = PendingAttachment(
                mediaType = "VIDEO",
                mediaUri = uri.toString(),
                fileName = "Video_${System.currentTimeMillis().toString().takeLast(4)}.mp4",
                fileSize = "Vídeo Criptografado"
            )
        }
    }

    // Android Document/File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Arquivo_${System.currentTimeMillis().toString().takeLast(4)}.dat"
            pendingAttachment = PendingAttachment(
                mediaType = "FILE",
                mediaUri = uri.toString(),
                fileName = fileName,
                fileSize = "Documento Seguro"
            )
        }
    }

    // Auto-scroll to bottom on new messages or input change
    LaunchedEffect(messages.size, textInput.isNotEmpty()) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(userFeedback) {
        userFeedback?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            onClearFeedback()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        },
        containerColor = ImmersiveSurface,
        topBar = {
            // Immersive Top App Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.ObsidianSurface)
                    .statusBarsPadding()
                    .border(0.5.dp, com.example.ui.theme.ObsidianBorder)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(com.example.ui.theme.ObsidianCardElevated)
                                .testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = com.example.ui.theme.TitaniumPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Gradient Avatar with hardware security ring
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            com.example.ui.theme.SecurityEmerald.copy(alpha = 0.2f),
                                            com.example.ui.theme.ObsidianCardElevated
                                        )
                                    )
                                )
                                .border(1.dp, com.example.ui.theme.SecurityEmerald.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = channel.name.take(2).uppercase(Locale.getDefault()),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = com.example.ui.theme.TitaniumPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = channel.name,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ImmersiveOnSurface,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(com.example.ui.theme.SecurityEmerald)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "E2EE • HARDWARE TEE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.SecurityEmerald,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Call Buttons, TTL Selector Chip & Options Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Voice Call Button
                        IconButton(
                            onClick = { activeCallType = CallType.AUDIO },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("audio_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Chamada de Voz",
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Video Call Button
                        IconButton(
                            onClick = { activeCallType = CallType.VIDEO },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Chamada de Vídeo",
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ImmersiveCardVariant)
                                    .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(8.dp))
                                    .clickable { showTtlMenu = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("ttl_selector_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Configurar TTL",
                                    tint = ImmersivePrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                val compactTtlLabel = when {
                                    selectedTtl.hours >= 1f -> "${selectedTtl.hours.toInt()}h"
                                    selectedTtl.hours >= 0.08f -> "5m"
                                    else -> "30s"
                                }
                                Text(
                                    text = compactTtlLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersivePrimary,
                                    maxLines = 1
                                )
                            }

                            // TTL Dropdown Menu
                            DropdownMenu(
                                expanded = showTtlMenu,
                                onDismissRequest = { showTtlMenu = false },
                                modifier = Modifier.background(ImmersiveHeader)
                            ) {
                                Text(
                                    text = "TEMPO DE EXPIRAÇÃO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                ttlPresets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = preset.label,
                                                    fontWeight = if (preset == selectedTtl) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (preset == selectedTtl) ImmersivePrimary else ImmersiveOnSurface,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = preset.description,
                                                    fontSize = 10.sp,
                                                    color = ImmersiveMutedLight
                                                )
                                            }
                                        },
                                        onClick = {
                                            onSelectTtl(preset)
                                            showTtlMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // More Options Menu
                        Box {
                            IconButton(
                                onClick = { showOptionsMenu = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opções", tint = ImmersiveOnSurface, modifier = Modifier.size(18.dp))
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false },
                                modifier = Modifier.background(ImmersiveHeader)
                            ) {
                                // Simulate Recipient Read Receipt for first unread sent message
                                val unreadSentMessage = messages.findLast { it.senderId == "ME" && !it.isRead }
                                if (unreadSentMessage != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Simular Leitura do Destinatário", color = ImmersivePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            showOptionsMenu = false
                                            onSimulateRead(unreadSentMessage.id)
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Limpar Chat (Shake to Clear)", color = ImmersivePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        onSimulateShake()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = EmberOrange, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Simular Captura de Tela (Teste)", color = EmberOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        onSimulateScreenshot()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ImmersiveExpiring, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Incinerar Conversa", color = ImmersiveExpiring, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        showIncinerateDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ImmersivePrimary.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    )
                )
        ) {
            // Expiry Info Pill & Screenshot Alert Banner
            if (isScreenshotLockdownActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(ImmersiveExpiring.copy(alpha = 0.25f), ImmersiveCard)
                                )
                            )
                            .border(1.2.dp, ImmersiveExpiring.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ImmersiveExpiring),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueio de Segurança",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BLOQUEIO DE SEGURANÇA ATIVO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ImmersiveExpiring,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Captura de tela detectada. Fotos 1x e notas secretas foram bloqueadas preventivamente.",
                                    fontSize = 11.sp,
                                    color = ImmersiveOnSurface,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onDismissScreenshotLockdown,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ImmersiveExpiring,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Liberar Proteção", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ImmersiveCardVariant)
                            .border(1.dp, ImmersiveOutline, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = ImmersiveMutedLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "As mensagens expiram em ${selectedTtl.label} após o envio",
                                fontSize = 11.sp,
                                color = ImmersiveMutedLight
                            )
                        }
                    }
                }
            }

            // Message Stream
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(ImmersiveCard)
                            .border(1.dp, ImmersiveOutline, RoundedCornerShape(20.dp))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoDelete,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Histórico Limpo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ImmersiveOnSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nenhuma mensagem ativa. Mensagens, fotos e arquivos somem automaticamente sem deixar vestígios.",
                            fontSize = 12.sp,
                            color = ImmersiveMuted,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            currentTime = currentTime,
                            onShredMessage = { onShredMessage(it) },
                            onCopyMessage = {},
                            isLockdownActive = isScreenshotLockdownActive,
                            onSimulateRead = { onSimulateRead(it) }
                        )
                    }
                }
            }

            // Interactive Simulation Helper Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ImmersivePrimary.copy(alpha = 0.1f))
                        .border(0.8.dp, ImmersivePrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { onSimulateReply() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("simulate_reply_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionAnswer,
                        contentDescription = "Simular Resposta",
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Simular Resposta",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        maxLines = 1
                    )
                }
            }

            // Bottom Composer Input Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ImmersiveSurface)
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (isRecordingAudio) {
                    AudioRecordingBar(
                        availableTtls = ttlPresets,
                        defaultTtl = selectedTtl,
                        onSendAudio = { duration, isViewOnce, customTtl ->
                            isRecordingAudio = false
                            onSendAudio(duration, isViewOnce, customTtl)
                        },
                        onCancelRecording = {
                            isRecordingAudio = false
                        }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Attachment (+) Button
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(com.example.ui.theme.ObsidianCardElevated)
                                .border(1.dp, com.example.ui.theme.ObsidianBorder, CircleShape)
                                .testTag("attachment_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Anexar Mídia",
                                tint = com.example.ui.theme.TitaniumPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Floating capsule input box container
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(26.dp))
                                .background(com.example.ui.theme.ObsidianCard)
                                .border(
                                    1.dp,
                                    if (isViewOnceMessage || isBurnerNoteMode) com.example.ui.theme.EmberFlame.copy(alpha = 0.7f)
                                    else com.example.ui.theme.ObsidianBorder,
                                    RoundedCornerShape(26.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // "1x" View-Once Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isViewOnceMessage) com.example.ui.theme.EmberFlame else Color.Transparent)
                                        .border(
                                            0.8.dp,
                                            if (isViewOnceMessage) com.example.ui.theme.EmberFlame else com.example.ui.theme.ObsidianBorder,
                                            CircleShape
                                        )
                                        .clickable { isViewOnceMessage = !isViewOnceMessage }
                                        .testTag("view_once_message_toggle"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "1x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isViewOnceMessage) Color.Black else com.example.ui.theme.TitaniumMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Burner Note Toggle Button
                                IconButton(
                                    onClick = { isBurnerNoteMode = !isBurnerNoteMode },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isBurnerNoteMode) com.example.ui.theme.EmberFlame.copy(alpha = 0.25f) else Color.Transparent)
                                        .testTag("burner_note_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Nota Secreta",
                                        tint = if (isBurnerNoteMode) com.example.ui.theme.EmberFlame else com.example.ui.theme.TitaniumMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                // Text Field
                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = {
                                        Text(
                                            text = when {
                                                isViewOnceMessage -> "1x Visualização única..."
                                                isBurnerNoteMode -> "Nota secreta temporária..."
                                                else -> "Mensagem protegida..."
                                            },
                                            color = if (isViewOnceMessage) com.example.ui.theme.EmberFlame.copy(alpha = 0.8f) else com.example.ui.theme.TitaniumMuted,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = ImmersiveOnSurface,
                                        unfocusedTextColor = ImmersiveOnSurface,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    maxLines = 4,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("message_input_field")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send Button (If text not blank) OR Audio Mic Record Button (If text is blank)
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendMessage(textInput, isBurnerNoteMode, isViewOnceMessage, null)
                                    textInput = ""
                                    isBurnerNoteMode = false
                                    isViewOnceMessage = false
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isViewOnceMessage || isBurnerNoteMode) com.example.ui.theme.EmberFlame
                                        else com.example.ui.theme.TitaniumPrimary
                                    )
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = if (isViewOnceMessage || isBurnerNoteMode) Color.Black else com.example.ui.theme.ObsidianBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { isRecordingAudio = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.ObsidianCardElevated)
                                    .border(1.dp, com.example.ui.theme.ObsidianBorder, CircleShape)
                                    .testTag("mic_record_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Gravar Áudio",
                                    tint = com.example.ui.theme.TitaniumPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Safe Area Indicator Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(70.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ImmersiveOutline.copy(alpha = 0.4f))
                )
            }
        }
    }

    // Attachment Picker Modal BottomSheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = sheetState,
            containerColor = ImmersiveHeader,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "ENVIAR MÍDIA PROTEGIDA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveMuted,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Photo Option
                    AttachmentOptionItem(
                        icon = Icons.Default.AddPhotoAlternate,
                        title = "Foto",
                        onClick = {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    // Video Option
                    AttachmentOptionItem(
                        icon = Icons.Default.Videocam,
                        title = "Vídeo",
                        onClick = {
                            showAttachmentSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )

                    // File Option
                    AttachmentOptionItem(
                        icon = Icons.Default.InsertDriveFile,
                        title = "Arquivo",
                        onClick = {
                            showAttachmentSheet = false
                            filePickerLauncher.launch("*/*")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "EXEMPLOS RÁPIDOS DE DEMONSTRAÇÃO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersivePrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Fast test media items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickMediaChip(
                        label = "📷 Foto Exemplo",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentSheet = false
                            pendingAttachment = PendingAttachment(
                                mediaType = "IMAGE",
                                mediaUri = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
                                fileName = "Design_Criptografado.jpg",
                                fileSize = "2.4 MB"
                            )
                        }
                    )

                    QuickMediaChip(
                        label = "📁 Documento PDF",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showAttachmentSheet = false
                            pendingAttachment = PendingAttachment(
                                mediaType = "FILE",
                                mediaUri = "content://safe/document.pdf",
                                fileName = "Contrato_Confidencial.pdf",
                                fileSize = "1.8 MB"
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Attachment Confirmation & Caption Dialog
    pendingAttachment?.let { attachment ->
        Dialog(onDismissRequest = { pendingAttachment = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ImmersiveHeader)
                    .border(1.dp, ImmersiveOutline, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enviar ${when(attachment.mediaType) { "IMAGE" -> "Foto"; "VIDEO" -> "Vídeo"; else -> "Arquivo" }}",
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveOnSurface,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = { pendingAttachment = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = ImmersiveMutedLight)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (attachment.mediaType == "IMAGE") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ImmersiveCardVariant)
                        ) {
                            AsyncImage(
                                model = attachment.mediaUri,
                                contentDescription = "Prévia da foto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ImmersiveCardVariant)
                                .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (attachment.mediaType == "VIDEO") Icons.Default.VideoFile else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = attachment.fileName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveOnSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${attachment.fileSize} • Expira em ${selectedTtl.label}",
                                    fontSize = 11.sp,
                                    color = ImmersiveMutedLight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // View-Once Toggle Pill for Media
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (attachment.isViewOnce) EmberOrange.copy(alpha = 0.15f) else ImmersiveCardVariant)
                            .border(1.dp, if (attachment.isViewOnce) EmberOrange.copy(alpha = 0.6f) else ImmersiveOutline, RoundedCornerShape(12.dp))
                            .clickable {
                                pendingAttachment = attachment.copy(isViewOnce = !attachment.isViewOnce)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("media_view_once_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (attachment.isViewOnce) EmberOrange else ImmersiveCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1x",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = if (attachment.isViewOnce) Color.Black else ImmersiveMutedLight
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Visualização Única (1x)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (attachment.isViewOnce) EmberOrange else ImmersiveOnSurface
                                )
                                Text(
                                    text = "Destrói após abrir uma vez",
                                    fontSize = 10.sp,
                                    color = ImmersiveMutedLight
                                )
                            }
                        }

                        Text(
                            text = if (attachment.isViewOnce) "ATIVADO" else "DESATIVADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (attachment.isViewOnce) EmberOrange else ImmersiveMutedLight
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom TTL preset selector for this media
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val activeTtlHours = attachment.customTtlHours ?: selectedTtl.hours
                        ttlPresets.take(4).forEach { preset ->
                            val isSelected = activeTtlHours == preset.hours
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ImmersivePrimary.copy(alpha = 0.2f) else ImmersiveCardVariant)
                                    .border(0.8.dp, if (isSelected) ImmersivePrimary else ImmersiveOutline, RoundedCornerShape(8.dp))
                                    .clickable {
                                        pendingAttachment = attachment.copy(customTtlHours = preset.hours)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ImmersivePrimary else ImmersiveMutedLight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = attachmentCaption,
                        onValueChange = { attachmentCaption = it },
                        placeholder = { Text("Adicionar legenda (opcional)...", fontSize = 12.sp, color = ImmersiveMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ImmersiveOnSurface,
                            unfocusedTextColor = ImmersiveOnSurface,
                            focusedBorderColor = ImmersivePrimary,
                            unfocusedBorderColor = ImmersiveOutline,
                            focusedContainerColor = ImmersiveCardVariant,
                            unfocusedContainerColor = ImmersiveCardVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { pendingAttachment = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = ImmersiveMutedLight)
                        }

                        Button(
                            onClick = {
                                onSendMedia(
                                    attachment.mediaType,
                                    attachment.mediaUri,
                                    attachment.fileName,
                                    attachment.fileSize,
                                    attachmentCaption,
                                    attachment.isViewOnce,
                                    attachment.customTtlHours
                                )
                                pendingAttachment = null
                                attachmentCaption = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).testTag("confirm_send_media_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = ImmersiveOnPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enviar Agora", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Incinerate Room Dialog
    if (showIncinerateDialog) {
        AlertDialog(
            onDismissRequest = { showIncinerateDialog = false },
            containerColor = ImmersiveHeader,
            icon = {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ImmersiveExpiring, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Incinerar Conversa e Contato?", fontWeight = FontWeight.Bold, color = ImmersiveExpiring, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Esta conversa e todas as mensagens trocadas serão excluídas e sobrescritas imediatamente. Nenhum registro ou histórico do contato permanecerá no aplicativo.",
                    color = ImmersiveOnSurface,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIncinerateDialog = false
                        onIncinerateRoom(channel.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveExpiring),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Incinerar Agora", color = Color(0xFF601410), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncinerateDialog = false }) {
                    Text("Cancelar", color = ImmersiveMutedLight)
                }
            }
        )
    }

    // Shake to Clear Confirmation Dialog
    if (shakeDialogVisible) {
        AlertDialog(
            onDismissRequest = { onDismissShakeDialog() },
            containerColor = ImmersiveHeader,
            icon = {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("📳 Shake to Clear Detectado", fontWeight = FontWeight.Bold, color = ImmersivePrimary, fontSize = 17.sp)
            },
            text = {
                Text(
                    "Chacoalhar detectado! Deseja apagar permanentemente todas as mensagens e mídias desta conversa aberta? Esta ação limpa o chat instantaneamente sem deixar vestígios (Zero Trace).",
                    color = ImmersiveOnSurface,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTriggerShakeWipe()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Limpar Histórico Agora", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissShakeDialog() }) {
                    Text("Cancelar", color = ImmersiveMutedLight)
                }
            }
        )
    }

    // Encrypted Audio / Video Call Interface
    if (activeCallType != null) {
        EncryptedCallDialog(
            channel = channel,
            callType = activeCallType!!,
            onDismiss = { activeCallType = null }
        )
    }
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(ImmersivePrimary.copy(alpha = 0.15f))
                .border(1.dp, ImmersivePrimary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = ImmersivePrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ImmersiveOnSurface
        )
    }
}

@Composable
fun QuickMediaChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveCardVariant)
            .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = ImmersiveOnSurface,
            maxLines = 1
        )
    }
}
