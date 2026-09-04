package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * Universal Multiplatform QR Code renderer using pure Kotlin qrose library.
 * Renders on Android, Desktop, iOS, and Web.
 */
@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    padding: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        val painter = rememberQrCodePainter(data = data)
        Image(
            painter = painter,
            contentDescription = "QR Code para pareamento presencial",
            modifier = Modifier.size(size)
        )
    }
}
