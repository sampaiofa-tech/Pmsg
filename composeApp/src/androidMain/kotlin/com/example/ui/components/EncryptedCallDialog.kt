package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.BurnerChannel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveAvatarDeep
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOnlineGreen
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import kotlinx.coroutines.delay
import java.util.Locale

enum class CallType {
    AUDIO,
    VIDEO
}

@Composable
fun EncryptedCallDialog(
    channel: BurnerChannel,
    callType: CallType,
    onDismiss: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(callType == CallType.VIDEO) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var isConnecting by remember { mutableStateOf(true) }

    // Call timer ticker
    LaunchedEffect(Unit) {
        delay(1200) // Realistic connection handshake
        isConnecting = false
        while (true) {
            delay(1000)
            callDurationSeconds++
        }
    }

    // Audio pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call_pulse_scale"
    )

    val minutes = callDurationSeconds / 60
    val seconds = callDurationSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ImmersiveSurface)
                .statusBarsPadding()
                .testTag("encrypted_call_screen")
        ) {
            // If Video Call and Video is active, show video streams
            if (isVideoEnabled) {
                // Remote Video Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF020617),
                                    Color(0xFF0B132B)
                                )
                            )
                        )
                ) {
                    // Contact mock remote stream visual
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=800&q=80",
                        contentDescription = "Vídeo do Contato",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alpha = 0.85f
                    )

                    // Local Camera Picture-in-Picture (PiP)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 70.dp, end = 16.dp)
                            .size(width = 100.dp, height = 140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ImmersiveCardVariant)
                            .border(1.dp, ImmersivePrimary, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80",
                            contentDescription = "Sua câmera",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("Você", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Audio Call Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ImmersivePrimary.copy(alpha = 0.15f),
                                    ImmersiveSurface,
                                    ImmersiveSurface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Pulsing Avatar Ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(170.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(if (isConnecting) 1f else pulseScale)
                                    .clip(CircleShape)
                                    .background(ImmersivePrimary.copy(alpha = 0.15f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ImmersivePrimary.copy(alpha = 0.4f), ImmersiveAvatarDeep)
                                        )
                                    )
                                    .border(2.dp, ImmersivePrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channel.name.take(2).uppercase(Locale.getDefault()),
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersivePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = channel.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveOnSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isConnecting) "Conectando chamada criptografada..." else timeFormatted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isConnecting) ImmersivePrimary else ImmersiveOnlineGreen
                        )
                    }
                }
            }

            // Top Header: Security badge & call status
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chamada Criptografada de Ponta a Ponta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ImmersiveOnSurface
                        )
                    }
                }

                if (isVideoEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = channel.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isConnecting) "Conectando vídeo..." else timeFormatted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnecting) ImmersivePrimary else ImmersiveOnlineGreen
                    )
                }
            }

            // Bottom Call Control Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.dp, ImmersiveOutline, RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Microphone
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else ImmersiveCardVariant)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Microfone",
                            tint = if (isMuted) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Toggle Camera (Video)
                    IconButton(
                        onClick = { isVideoEnabled = !isVideoEnabled },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (!isVideoEnabled && callType == CallType.VIDEO) Color.White else ImmersiveCardVariant)
                    ) {
                        Icon(
                            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Câmera de Vídeo",
                            tint = if (!isVideoEnabled && callType == CallType.VIDEO) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Speakerphone
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) ImmersivePrimary.copy(alpha = 0.3f) else ImmersiveCardVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Alto-falante",
                            tint = if (isSpeakerOn) ImmersivePrimary else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // End Call Button (Red)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ImmersiveExpiring)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Encerrar Chamada",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Zero registros gravados em servidores",
                    fontSize = 11.sp,
                    color = ImmersiveMutedLight
                )
            }
        }
    }
}
