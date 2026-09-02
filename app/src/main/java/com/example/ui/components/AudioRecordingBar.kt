package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveCard
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveHeader
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.viewmodel.TtlPreset
import kotlinx.coroutines.delay

@Composable
fun AudioRecordingBar(
    availableTtls: List<TtlPreset>,
    defaultTtl: TtlPreset,
    onSendAudio: (durationSeconds: Int, isViewOnce: Boolean, customTtlHours: Float) -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isViewOnce by remember { mutableStateOf(false) }
    var selectedTtl by remember { mutableStateOf(defaultTtl) }
    var showTtlDropdown by remember { mutableStateOf(false) }

    // Recording timer loop
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            recordingSeconds += 1
        }
    }

    // Pulsing REC animation
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        if (isViewOnce) EmberOrange.copy(alpha = 0.18f) else ImmersiveExpiring.copy(alpha = 0.15f),
                        ImmersiveCard
                    )
                )
            )
            .border(
                1.dp,
                if (isViewOnce) EmberOrange.copy(alpha = 0.6f) else ImmersiveExpiring.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cancel / Trash Button
            IconButton(
                onClick = onCancelRecording,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ImmersiveCardVariant)
                    .testTag("cancel_recording_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Cancelar Gravação",
                    tint = ImmersiveExpiring,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Pulsing Red REC indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(if (isViewOnce) EmberOrange else ImmersiveExpiring)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Timer display
            val mins = recordingSeconds / 60
            val secs = recordingSeconds % 60
            Text(
                text = String.format("%02d:%02d", mins, secs),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isViewOnce) EmberOrange else ImmersiveExpiring
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Animated live sound wave bars
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val heights = listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.5f, 0.8f, 0.3f, 0.6f, 0.95f, 0.4f, 0.7f, 0.5f)
                heights.forEachIndexed { i, h ->
                    val dynamicH = remember(recordingSeconds, i) {
                        val offset = (recordingSeconds * 3 + i) % heights.size
                        (heights[offset] * 18).coerceIn(4f, 18f).dp
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(dynamicH)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isViewOnce) EmberOrange else ImmersivePrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // "1x" View Once Button Toggle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isViewOnce) EmberOrange else ImmersiveCardVariant)
                    .border(
                        1.dp,
                        if (isViewOnce) EmberOrange else ImmersiveOutline,
                        CircleShape
                    )
                    .clickable { isViewOnce = !isViewOnce }
                    .testTag("audio_view_once_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1x",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isViewOnce) Color.Black else ImmersiveMutedLight
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // TTL Selector Menu
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ImmersiveCardVariant)
                        .border(0.8.dp, ImmersiveOutline, RoundedCornerShape(12.dp))
                        .clickable { showTtlDropdown = true }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                        .testTag("audio_ttl_dropdown_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = selectedTtl.label.replace(" Horas", "h").replace(" Minutos", "m").replace(" Min", "m").replace(" Segundos", "s"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveOnSurface
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTtlDropdown,
                    onDismissRequest = { showTtlDropdown = false },
                    modifier = Modifier.background(ImmersiveHeader)
                ) {
                    availableTtls.forEach { preset ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = preset.label,
                                    color = if (preset == selectedTtl) ImmersivePrimary else ImmersiveOnSurface,
                                    fontSize = 12.sp,
                                    fontWeight = if (preset == selectedTtl) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedTtl = preset
                                showTtlDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Audio Button
            IconButton(
                onClick = {
                    val finalSecs = recordingSeconds.coerceAtLeast(1)
                    onSendAudio(finalSecs, isViewOnce, selectedTtl.hours)
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isViewOnce) EmberOrange else ImmersivePrimary)
                    .testTag("send_audio_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar Áudio",
                    tint = if (isViewOnce) Color.Black else ImmersiveOnPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
