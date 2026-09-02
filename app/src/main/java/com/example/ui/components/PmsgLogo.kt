package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PmsgBorder
import com.example.ui.theme.PmsgMatteDark
import com.example.ui.theme.PmsgPlatinum

/**
 * Geometric P Logo icon badge according to brand specifications.
 */
@Composable
fun PmsgLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 22.dp,
    shapeRadius: Dp = 10.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(shapeRadius))
            .background(PmsgMatteDark)
            .border(1.dp, PmsgBorder, RoundedCornerShape(shapeRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_pmsg_logo),
            contentDescription = "Pmsg Logo",
            tint = Color.Unspecified, // Uses exact vector colors: #D4D4D8 and #52525B
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Wordmark: Sans-serif geométrica neutra (Pmsg), com entrelinhamento largo e peso Medium.
 */
@Composable
fun PmsgWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    color: Color = PmsgPlatinum
) {
    Text(
        text = "Pmsg",
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 3.5.sp,
        color = color,
        modifier = modifier
    )
}

/**
 * Full Brand Header unit (Icon + Wordmark).
 */
@Composable
fun PmsgBrandHeader(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 38.dp,
    wordmarkSize: TextUnit = 20.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PmsgLogoBadge(size = badgeSize)
        Spacer(modifier = Modifier.width(12.dp))
        PmsgWordmark(fontSize = wordmarkSize)
    }
}
