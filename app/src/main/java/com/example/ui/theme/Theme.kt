package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = ImmersiveSecondary,
    onSecondary = ImmersiveSurface,
    secondaryContainer = ImmersiveCardVariant,
    onSecondaryContainer = ImmersivePrimary,
    tertiary = EmberOrange,
    onTertiary = ImmersiveSurface,
    error = ImmersiveExpiring,
    onError = Color(0xFF601410),
    background = ImmersiveSurface,
    onBackground = ImmersiveOnSurface,
    surface = ImmersiveHeader,
    onSurface = ImmersiveOnSurface,
    surfaceVariant = ImmersiveCard,
    onSurfaceVariant = ImmersiveMutedLight,
    outline = ImmersiveOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF006D3E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF96F7B8),
    onSecondaryContainer = Color(0xFF00210F),
    tertiary = Color(0xFF944A00),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF171B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171B20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF72787E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek privacy stealth dark theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

