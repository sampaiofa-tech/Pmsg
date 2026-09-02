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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EphemeralMessage
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveCardVariant
import com.example.ui.theme.ImmersiveMuted
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersiveOnSurface
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersivePrimary
import kotlinx.coroutines.delay

@Composable
fun AudioMessageBubble(
    message: EphemeralMessage,
    isMe: Boolean,
    onShredMessage: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDuration = if (message.audioDurationSeconds > 0) message.audioDurationSeconds else 8
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgressSeconds by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var hasFinishedPlayingOnce by remember { mutableStateOf(false) }

    // Waveform heights seed
    val waveBars = remember(message.id) {
        val count = 22
        val random = java.util.Random(message.id + 100)
        List(count) { (0.25f + random.nextFloat() * 0.75f) }
    }

    // Playback loop
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (isPlaying && currentProgressSeconds < totalDuration) {
                val delayTime = (1000L / playbackSpeed).toLong()
                delay(delayTime)
                currentProgressSeconds += 1
            }
            if (currentProgressSeconds >= totalDuration) {
                isPlaying = false
                currentProgressSeconds = 0
                hasFinishedPlayingOnce = true
                // If it's a View-Once Audio, incinerate immediately after playback!
                if (message.isViewOnce) {
                    delay(800)
                    onShredMessage(message.id)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ImmersiveCardVariant.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        // View-Once Badge header if applicable
        if (message.isViewOnce) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(EmberOrange.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(EmberOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ÁUDIO DE VISUALIZAÇÃO ÚNICA",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmberOrange,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = EmberOrange,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Main Audio Player Control Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play / Pause Circle Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (message.isViewOnce) EmberOrange else ImmersivePrimary)
                    .clickable {
                        isPlaying = !isPlaying
                    }
                    .testTag("audio_play_pause_${message.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Ouvir Áudio",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Waveform visualizer
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playedFraction = if (totalDuration > 0) currentProgressSeconds.toFloat() / totalDuration.toFloat() else 0f
                    val playedBarsCount = (waveBars.size * playedFraction).toInt()

                    waveBars.forEachIndexed { index, heightMultiplier ->
                        val isPlayed = index <= playedBarsCount
                        val barHeight = (heightMultiplier * 24).coerceIn(4f, 24f).dp
                        val barColor = when {
                            isPlayed && message.isViewOnce -> EmberOrange
                            isPlayed -> ImmersivePrimary
                            else -> ImmersiveMutedLight.copy(alpha = 0.45f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Progress Time & Speed selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displaySeconds = if (isPlaying) currentProgressSeconds else totalDuration
                    val mins = displaySeconds / 60
                    val secs = displaySeconds % 60
                    val formattedTime = String.format("%02d:%02d", mins, secs)

                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ImmersiveMutedLight
                    )

                    // Playback speed pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ImmersiveOutline.copy(alpha = 0.3f))
                            .clickable {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                            }
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isViewOnce) EmberOrange else ImmersivePrimary
                        )
                    }
                }
            }
        }

        // View Once notice subtitle
        if (message.isViewOnce) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🔥 Auto-destruição imediata após reproduzir",
                fontSize = 9.sp,
                color = EmberOrange.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
