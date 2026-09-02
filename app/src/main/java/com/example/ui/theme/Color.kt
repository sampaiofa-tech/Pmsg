package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Pmsg Official Brand Specifications: Stealth Obsidian & Brushed Titanium
// =========================================================================
val PmsgMatteDark = Color(0xFF111215)       // Fundo: Grafite Obsidiana Profundo (#111215)
val PmsgPlatinum = Color(0xFFE4E4E7)        // Elemento Principal: Platina Titânio (#E4E4E7)
val PmsgEvasionGray = Color(0xFF52525B)     // Detalhe Neutro (#52525B)
val PmsgBorder = Color(0xFF26282E)          // Borda Refinada (#26282E)
val PmsgCardBg = Color(0xFF181A1F)          // Superfície Elevada (#181A1F)

// =========================================================================
// Harmonious Obsidian & Titanium Semantic Tokens
// =========================================================================
val ObsidianBlack = Color(0xFF0D0E11)
val ObsidianSurface = Color(0xFF14161A)
val ObsidianCard = Color(0xFF191B20)
val ObsidianCardElevated = Color(0xFF21242B)
val ObsidianBorder = Color(0xFF282B33)
val ObsidianBorderSubtle = Color(0x18FFFFFF)

val TitaniumPrimary = Color(0xFFE4E4E7)
val TitaniumSecondary = Color(0xFFA1A1AA)
val TitaniumMuted = Color(0xFF71717A)

val SecurityEmerald = Color(0xFF10B981)
val SecurityEmeraldContainer = Color(0xFF064E3B)
val EmberFlame = Color(0xFFF59E0B)
val EmberFlameContainer = Color(0xFF451A03)
val IncinerateCrimson = Color(0xFFEF4444)
val IncinerateCrimsonBg = Color(0xFF2C1316)

// Chat Bubbles
val BubbleUser = Color(0xFF23262E)
val BubbleUserBorder = Color(0xFF333742)
val BubbleContact = Color(0xFF17191E)
val BubbleContactBorder = Color(0xFF24272F)

// =========================================================================
// Immersive UI Palette (Maintained for total backward compatibility)
// =========================================================================
val ImmersiveSurface = ObsidianBlack
val ImmersiveHeader = ObsidianSurface
val ImmersiveCard = ObsidianCard
val ImmersiveCardVariant = ObsidianCardElevated
val ImmersivePrimary = TitaniumPrimary
val ImmersivePrimaryContainer = Color(0xFF2D3039)
val ImmersiveOnPrimary = ObsidianBlack
val ImmersiveSecondary = TitaniumSecondary
val ImmersiveOutline = ObsidianBorder
val ImmersiveOnSurface = Color(0xFFF4F4F5)
val ImmersiveMuted = TitaniumMuted
val ImmersiveMutedLight = TitaniumSecondary
val ImmersiveExpiring = IncinerateCrimson
val ImmersiveOnlineGreen = SecurityEmerald
val ImmersiveAvatarDeep = Color(0xFF20232A)

// Mappings for theme interoperability
val StealthBlack = ImmersiveSurface
val StealthDarkSurface = ImmersiveHeader
val StealthCardSurface = ImmersiveCard
val StealthCardSurfaceLight = ImmersiveCardVariant

// Accents
val ElectricCyan = ImmersivePrimary
val ElectricCyanDim = ImmersiveSecondary
val NeonEmerald = ImmersiveOnlineGreen
val EmberOrange = EmberFlame
val IncinerateRed = ImmersiveExpiring
val GhostPurple = ImmersivePrimary

// Text & Neutral Colors
val TextPrimaryDark = ImmersiveOnSurface
val TextSecondaryDark = ImmersiveMutedLight
val TextMutedDark = ImmersiveMuted
val BorderSubtleDark = ImmersiveOutline
val GlowOverlay = Color(0x1AE4E4E7)
