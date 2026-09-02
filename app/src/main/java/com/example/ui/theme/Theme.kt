package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TitaniumPrimary,
    onPrimary = ObsidianBlack,
    primaryContainer = ImmersivePrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = TitaniumSecondary,
    onSecondary = ObsidianSurface,
    secondaryContainer = ObsidianCardElevated,
    onSecondaryContainer = TitaniumPrimary,
    tertiary = SecurityEmerald,
    onTertiary = ObsidianBlack,
    tertiaryContainer = SecurityEmeraldContainer,
    onTertiaryContainer = SecurityEmerald,
    error = IncinerateCrimson,
    onError = Color.White,
    errorContainer = IncinerateCrimsonBg,
    onErrorContainer = IncinerateCrimson,
    background = ObsidianBlack,
    onBackground = ImmersiveOnSurface,
    surface = ObsidianSurface,
    onSurface = ImmersiveOnSurface,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TitaniumSecondary,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E293B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1F5F9),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    error = Color(0xFFDC2626),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to stealth obsidian privacy dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
