package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.ImmersiveExpiring
import com.example.ui.theme.ImmersiveMutedLight
import com.example.ui.theme.ImmersivePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated visual particle disintegration / vaporizing effect overlay.
 * Renders glowing disintegrating cyber/ember dust particles rising and dissolving
 * when a message has been read and is in the process of self-destruction / disappearing.
 */
@Composable
fun DisappearingParticleOverlay(
    modifier: Modifier = Modifier,
    isRead: Boolean = true,
    isUrgent: Boolean = false,
    disappearProgress: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particle_transition")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    // Pre-calculated deterministic pseudo-random particle seed coordinates
    val particles = remember {
        List(18) {
            ParticleData(
                relX = Random.nextFloat(),
                relY = Random.nextFloat(),
                radius = Random.nextFloat() * 2.8f + 1.2f,
                speed = Random.nextFloat() * 0.7f + 0.5f,
                colorType = Random.nextInt(3)
            )
        }
    }

    val baseCyan = ImmersivePrimary // 0xFF00E5FF
    val emberOrange = EmberOrange
    val purpleGlow = Color(0xFFD946EF)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Shimmer vaporization laser sweep across the top/bottom edges
        if (isRead) {
            val sweepX = width * shimmerOffset
            val sweepBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    (if (isUrgent) emberOrange else baseCyan).copy(alpha = 0.28f),
                    Color.Transparent
                ),
                start = Offset(sweepX - 80f, 0f),
                end = Offset(sweepX + 80f, height)
            )
            drawRect(brush = sweepBrush)
        }

        // Particle floating and disintegration
        particles.forEachIndexed { i, p ->
            val localPhase = (phase * p.speed + (i * 0.07f)) % 1f
            val currentY = (height * p.relY - (localPhase * height * 0.85f))
            val adjustedY = if (currentY < 0) height + currentY else currentY
            
            val waveX = sin((localPhase * 2 * Math.PI + i).toFloat()) * 12f
            val currentX = (width * p.relX + waveX).coerceIn(0f, width)

            val alpha = ((1f - (adjustedY / height)) * (1f - localPhase) * 0.9f).coerceIn(0.1f, 0.95f)

            val particleColor = when (p.colorType) {
                0 -> baseCyan.copy(alpha = alpha)
                1 -> if (isUrgent) emberOrange.copy(alpha = alpha) else Color(0xFF67E8F9).copy(alpha = alpha)
                else -> if (isUrgent) ImmersiveExpiring.copy(alpha = alpha) else purpleGlow.copy(alpha = alpha * 0.8f)
            }

            drawCircle(
                color = particleColor,
                radius = p.radius * (1f + localPhase * 0.5f),
                center = Offset(currentX, adjustedY)
            )
        }
    }
}

private data class ParticleData(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val speed: Float,
    val colorType: Int
)

/**
 * Visual Read Receipt Badge with multi-state icons and vanishing countdown indicator.
 */
@Composable
fun ReadReceiptStatusBadge(
    isMe: Boolean,
    isDelivered: Boolean,
    isRead: Boolean,
    readAt: Long?,
    formattedSentTime: String,
    remainingMillis: Long,
    isUrgent: Boolean,
    disappearAfterReadSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedReadTime = remember(readAt) {
        if (readAt != null && readAt > 0) timeFormatter.format(Date(readAt)) else null
    }

    val secondsRemaining = (remainingMillis / 1000).coerceAtLeast(0)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (isMe) {
            // Sent by ME: Show status progression (Sent -> Delivered -> Read) + Disappearing indicator
            when {
                isRead -> {
                    // Double check with vibrant glowing cyan/teal
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ImmersivePrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Lido pelo destinatário",
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (formattedReadTime != null) "Lido $formattedReadTime" else "Lido",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersivePrimary
                        )

                        if (disappearAfterReadSeconds > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Desaparecendo",
                                tint = EmberOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${secondsRemaining}s",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = EmberOrange
                            )
                        }
                    }
                }
                isDelivered -> {
                    // Double check in neutral grey
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedSentTime,
                            fontSize = 10.sp,
                            color = ImmersiveMutedLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Entregue",
                            tint = ImmersiveMutedLight,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                else -> {
                    // Single check (Sent to server)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formattedSentTime,
                            fontSize = 10.sp,
                            color = ImmersiveMutedLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Enviado",
                            tint = ImmersiveMutedLight,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        } else {
            // Received from Contact: Show sent time + read & vanishing indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedSentTime,
                    fontSize = 10.sp,
                    color = ImmersiveMutedLight
                )

                if (isRead && disappearAfterReadSeconds > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmberOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Desaparecendo",
                            tint = EmberOrange,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Some em ${secondsRemaining}s",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmberOrange
                        )
                    }
                }
            }
        }
    }
}
