package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.EphemeralMessage
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveHeader
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: EphemeralMessage,
    currentTime: Long,
    onShredMessage: (Long) -> Unit,
    onCopyMessage: (String) -> Unit,
    isLockdownActive: Boolean = false,
    onSimulateRead: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedSentTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    // DEDICATED SCREENSHOT SECURITY ALERT BUBBLE
    if (message.mediaType == "SECURITY_ALERT") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(EmberOrange.copy(alpha = 0.22f), ImmersiveCard)
                        )
                    )
                    .border(1.dp, EmberOrange.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(EmberOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color.Black,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALERTA: CAPTURA DE TELA DETECTADA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = EmberOrange,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.content,
                    fontSize = 12.sp,
                    color = ImmersiveOnSurface,
                    lineHeight = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detectado às $formattedSentTime",
                        fontSize = 10.sp,
                        color = ImmersiveMutedLight
                    )
                    IconButton(
                        onClick = { onShredMessage(message.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar Alerta",
                            tint = ImmersiveMutedLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        return
    }

    val isMe = message.senderId == "ME"
    val remainingMillis = message.remainingMillis(currentTime)
    val progress = message.expirationProgress(currentTime)
    val isBurnerNote = message.mediaType == "BURNER_NOTE"
    var isRevealed by remember { mutableStateOf(!isBurnerNote) }
    var showMenu by remember { mutableStateOf(false) }
    var showFullImageViewer by remember { mutableStateOf(false) }
    var showViewOnceViewer by remember { mutableStateOf(false) }
    var showLockdownOverrideDialog by remember { mutableStateOf(false) }

    val isUrgent = remainingMillis < 60_000L

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    val bubbleBgColor = when {
        isUrgent -> ImmersiveExpiring.copy(alpha = 0.22f)
        isMe -> ImmersivePrimaryContainer
        else -> ImmersiveCard
    }

    val borderStrokeColor = when {
        isUrgent -> ImmersiveExpiring.copy(alpha = 0.8f)
        isMe -> ImmersivePrimary.copy(alpha = 0.35f)
        else -> ImmersiveOutline.copy(alpha = 0.5f)
    }

    // Expiry text calculation - accurately shows seconds when < 1 minute (e.g. 30s)
    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val expiryText = when {
        hours > 0 -> "Expira em ${hours}h"
        minutes > 0 -> "Expira em ${minutes}m ${seconds}s"
        totalSeconds > 0 -> "Expira em ${totalSeconds}s"
        else -> "Expirando agora"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            // Sender name (for received messages)
            if (!isMe) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersivePrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "E2EE",
                        tint = ImmersivePrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Bubble Container
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBgColor)
                    .border(0.8.dp, borderStrokeColor, bubbleShape)
                    .combinedClickable(
                        onClick = {
                            if (isBurnerNote && !isRevealed) {
                                if (isLockdownActive) {
                                    showLockdownOverrideDialog = true
                                } else {
                                    isRevealed = true
                                }
                            }
                        },
                        onLongClick = {
                            showMenu = true
                        }
                    )
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(
                        if (message.mediaType == "IMAGE") 6.dp else 12.dp
                    )
                ) {
                    // Burner Note Header if applicable
                    if (isBurnerNote) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EmberOrange.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Nota Autodestrutiva",
                                tint = EmberOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NOTA SECRETA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmberOrange,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // --- MEDIA CONTENT RENDERING ---
                    when {
                        // AUDIO VOICE MESSAGE
                        message.mediaType == "AUDIO" -> {
                            AudioMessageBubble(
                                message = message,
                                isMe = isMe,
                                onShredMessage = onShredMessage
                            )
                        }

                        // VIEW ONCE PHOTO
                        message.isViewOnce && message.mediaType == "IMAGE" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLockdownActive) ImmersiveExpiring.copy(alpha = 0.18f) else EmberOrange.copy(alpha = 0.15f))
                                    .border(1.dp, if (isLockdownActive) ImmersiveExpiring.copy(alpha = 0.8f) else EmberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isLockdownActive) {
                                            showLockdownOverrideDialog = true
                                        } else {
                                            showViewOnceViewer = true
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isLockdownActive) ImmersiveExpiring else EmberOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLockdownActive) {
                                        Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text("1", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isLockdownActive) "Foto Bloqueada (Print Detectado)" else "Foto confidencial",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLockdownActive) ImmersiveExpiring else EmberOrange,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isLockdownActive) "Toque para verificar segurança" else "Visualização única • Toque para ver",
                                        color = ImmersiveMutedLight,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isLockdownActive) Icons.Default.Shield else Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (isLockdownActive) ImmersiveExpiring else EmberOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // VIEW ONCE TEXT / NOTE
                        message.isViewOnce -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLockdownActive) ImmersiveExpiring.copy(alpha = 0.18f) else EmberOrange.copy(alpha = 0.15f))
                                    .border(1.dp, if (isLockdownActive) ImmersiveExpiring.copy(alpha = 0.8f) else EmberOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isLockdownActive) {
                                            showLockdownOverrideDialog = true
                                        } else {
                                            showViewOnceViewer = true
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isLockdownActive) ImmersiveExpiring else EmberOrange),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLockdownActive) {
                                        Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = Color.White, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text("1", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isLockdownActive) "Mensagem Bloqueada (Print Detectado)" else "Mensagem confidencial",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLockdownActive) ImmersiveExpiring else EmberOrange,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isLockdownActive) "Toque para verificar segurança" else "Visualização única • Toque para abrir",
                                        color = ImmersiveMutedLight,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isLockdownActive) Icons.Default.Shield else Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (isLockdownActive) ImmersiveExpiring else EmberOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // STANDARD IMAGE
                        message.mediaType == "IMAGE" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ImmersiveCardVariant)
                                    .clickable { showFullImageViewer = true }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(message.mediaUri ?: "")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto criptografada",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Tap to expand badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Toque para ampliar",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.content,
                                    color = if (isMe) Color.White else ImmersiveOnSurface,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // STANDARD VIDEO
                        message.mediaType == "VIDEO" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        )
                                    )
                                    .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(12.dp))
                                    .clickable {
                                        message.mediaUri?.let { uriStr ->
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(uriStr), "video/*")
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // Fallback
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(ImmersivePrimary.copy(alpha = 0.25f))
                                            .border(1.dp, ImmersivePrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Reproduzir Vídeo",
                                            tint = ImmersivePrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = message.fileName ?: "Vídeo Criptografado",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = message.fileSize ?: "Vídeo Seguro",
                                        fontSize = 10.sp,
                                        color = ImmersiveMutedLight
                                    )
                                }
                            }
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.content,
                                    color = if (isMe) Color.White else ImmersiveOnSurface,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // STANDARD FILE
                        message.mediaType == "FILE" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ImmersiveCardVariant)
                                    .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(10.dp))
                                    .clickable {
                                        message.mediaUri?.let { uriStr ->
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(Uri.parse(uriStr), "*/*")
                                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // Fallback
                                            }
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ImmersivePrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "Arquivo",
                                        tint = ImmersivePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: "Arquivo Seguro",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveOnSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${message.fileSize ?: "Arquivo"} • Criptografado",
                                        fontSize = 10.sp,
                                        color = ImmersiveMutedLight
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Abrir",
                                    tint = ImmersivePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.content,
                                    color = if (isMe) Color.White else ImmersiveOnSurface,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        else -> {
                            // Text or Burner note content
                            if (isBurnerNote && !isRevealed) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VisibilityOff,
                                        contentDescription = "Toque para Revelar",
                                        tint = ImmersivePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Toque para revelar nota secreta",
                                        fontSize = 13.sp,
                                        color = ImmersivePrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    color = if (isMe) Color.White else ImmersiveOnSurface,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Countdown & Shred action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CountdownBadge(
                            remainingMillis = remainingMillis,
                            progress = progress
                        )

                        IconButton(
                            onClick = { onShredMessage(message.id) },
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("shred_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Triturar",
                                tint = if (isUrgent) ImmersiveExpiring else ImmersiveMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Disappearing Vapor/Ember Particle Overlay when message is read
                if (message.isRead) {
                    DisappearingParticleOverlay(
                        isRead = true,
                        isUrgent = isUrgent,
                        disappearProgress = progress
                    )
                }

                // Expiry Progress Bar (2px progress line at bottom of bubble)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(fraction = (1f - progress).coerceIn(0.02f, 1f))
                        .height(2.dp)
                        .background(if (isMe) Color.White.copy(alpha = 0.35f) else ImmersivePrimary.copy(alpha = 0.45f))
                )

                // Dropdown Menu on Long Press
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(ImmersiveHeader)
                ) {
                    // Simulate Read Receipt trigger
                    if (isMe && !message.isRead && onSimulateRead != null) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Simular Leitura do Destinatário", color = ImmersivePrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onSimulateRead(message.id)
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ImmersiveExpiring, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Incinerar Agora (Zero Rastro)", color = ImmersiveExpiring, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            showMenu = false
                            onShredMessage(message.id)
                        }
                    )

                    if (message.content.isNotBlank()) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copiar Texto", color = ImmersiveOnSurface, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Pmsg", message.content)
                                clipboard.setPrimaryClip(clip)
                                onCopyMessage(message.content)
                            }
                        )
                    }
                }
            }

            // Expiry & Read Receipt info below bubble
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (isMe) {
                    Text(
                        text = "$expiryText •",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUrgent) ImmersiveExpiring else ImmersivePrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ReadReceiptStatusBadge(
                        isMe = true,
                        isDelivered = message.isDelivered,
                        isRead = message.isRead,
                        readAt = message.readAt,
                        formattedSentTime = formattedSentTime,
                        remainingMillis = remainingMillis,
                        isUrgent = isUrgent,
                        disappearAfterReadSeconds = message.disappearAfterReadSeconds
                    )
                } else {
                    ReadReceiptStatusBadge(
                        isMe = false,
                        isDelivered = true,
                        isRead = message.isRead,
                        readAt = message.readAt,
                        formattedSentTime = formattedSentTime,
                        remainingMillis = remainingMillis,
                        isUrgent = isUrgent,
                        disappearAfterReadSeconds = message.disappearAfterReadSeconds
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• $expiryText",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUrgent) ImmersiveExpiring else ImmersivePrimary
                    )
                }
            }
        }
    }

    // View-Once Viewer Dialog
    if (showViewOnceViewer) {
        ViewOnceViewerDialog(
            message = message,
            onDismissAndShred = {
                showViewOnceViewer = false
                onShredMessage(message.id)
            }
        )
    }

    // Full screen image viewer dialog
    if (showFullImageViewer && message.mediaUri != null) {
        Dialog(
            onDismissRequest = { showFullImageViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(message.mediaUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                IconButton(
                    onClick = { showFullImageViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Safety Lockdown Override Confirmation Dialog
    if (showLockdownOverrideDialog) {
        Dialog(onDismissRequest = { showLockdownOverrideDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ImmersiveCard)
                    .border(1.dp, ImmersiveExpiring.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ImmersiveExpiring.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Bloqueio de Segurança",
                            tint = ImmersiveExpiring,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Conteúdo Sensível Bloqueado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ImmersiveOnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Uma captura de tela foi detectada recentemente nesta sala. Para garantir sua privacidade, a visualização única e notas secretas foram protegidas.",
                        fontSize = 12.sp,
                        color = ImmersiveMutedLight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                showLockdownOverrideDialog = false
                                onShredMessage(message.id)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveExpiring.copy(alpha = 0.5f))
                        ) {
                            Text("Triturar", color = ImmersiveExpiring, fontSize = 12.sp)
                        }

                        androidx.compose.material3.Button(
                            onClick = {
                                showLockdownOverrideDialog = false
                                if (message.isViewOnce) {
                                    showViewOnceViewer = true
                                } else if (isBurnerNote) {
                                    isRevealed = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = EmberOrange,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Abrir Mesmo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
