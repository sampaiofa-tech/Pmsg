package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.IncinerateRed
import java.util.Locale

@Composable
fun CountdownBadge(
    remainingMillis: Long,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val isUrgent = totalSeconds < 60
    val isWarning = totalSeconds < 3600 && !isUrgent

    // Pulsing animation for critical expiration countdown (< 60s)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isUrgent) {
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(400),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1000)),
            label = "staticAlpha"
        )
    }

    val badgeColor by animateColorAsState(
        targetValue = when {
            isUrgent -> IncinerateRed
            isWarning -> EmberOrange
            else -> ElectricCyan
        },
        label = "badgeColor"
    )

    val timeFormatted = when {
        hours > 0 -> String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.getDefault(), "%02dm %02ds", minutes, seconds)
        else -> String.format(Locale.getDefault(), "%02ds", seconds)
    }

    Box(
        modifier = modifier
            .alpha(pulseAlpha)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor.copy(alpha = 0.14f))
            .border(1.dp, badgeColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("countdown_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isUrgent) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Queimando",
                    tint = IncinerateRed,
                    modifier = Modifier.size(14.dp)
                )
            } else if (isWarning) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = "Tempo acabando",
                    tint = EmberOrange,
                    modifier = Modifier.size(13.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Temporizador 24h",
                    tint = ElectricCyan,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = timeFormatted,
                color = badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Micro circular progress indicator showing time drain
            CircularProgressIndicator(
                progress = { (1f - progress).coerceIn(0.05f, 1f) },
                modifier = Modifier.size(10.dp),
                color = badgeColor,
                trackColor = badgeColor.copy(alpha = 0.2f),
                strokeWidth = 1.5.dp
            )
        }
    }
}
