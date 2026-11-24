package com.communitycare.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Brand colours ──────────────────────────────────────────────────────────
val TealPrimary    = Color(0xFF0A9396)
val TealDark       = Color(0xFF005F73)
val TealLight      = Color(0xFF94D2BD)
val TealBackground = Color(0xFFE0F4F4)
val TealSurface    = Color(0xFFF0FAFA)
val AccentGreen    = Color(0xFF52B788)
val AccentAmber    = Color(0xFFF4A261)
val AccentRed      = Color(0xFFE63946)
val TextPrimary    = Color(0xFF023047)
val TextSecondary  = Color(0xFF5F7A8A)
val White          = Color(0xFFFFFFFF)
val LightGray      = Color(0xFFF5F5F5)
val CardBg         = Color(0xFFFFFFFF)

private val CommunityCareColorScheme = lightColorScheme(
    primary          = TealPrimary,
    onPrimary        = White,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary        = TealDark,
    onSecondary      = White,
    secondaryContainer = TealLight,
    onSecondaryContainer = TealDark,
    tertiary = TealPrimary,
    onTertiary = White,
    tertiaryContainer = TealBackground,
    onTertiaryContainer = TealDark,
    surfaceVariant = TealSurface,
    onSurfaceVariant = TextSecondary,
    surfaceTint = TealPrimary,
    outline = Color(0xFF688589),
    background       = TealBackground,
    surface          = CardBg,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    error            = AccentRed
)

@Composable
fun CommunityCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CommunityCareColorScheme,
        content = content
    )
}
