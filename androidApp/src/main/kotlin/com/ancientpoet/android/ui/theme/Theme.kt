package com.ancientpoet.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ancient Chinese aesthetic palette
val InkBlack = Color(0xFF1A1A2E)
val RicePaper = Color(0xFFF5F0E8)
val VermilionRed = Color(0xFFC84C3D)
val ImperialGold = Color(0xFFC8A45C)
val JadeGreen = Color(0xFF6B8E7B)
val SkyBlue = Color(0xFF7BA7BC)
val WarmGray = Color(0xFF8B8178)

private val LightColors = lightColorScheme(
    primary = VermilionRed,
    onPrimary = RicePaper,
    primaryContainer = Color(0xFFFDE8E5),
    secondary = ImperialGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF0DC),
    tertiary = JadeGreen,
    background = RicePaper,
    onBackground = InkBlack,
    surface = Color.White,
    onSurface = InkBlack,
    surfaceVariant = Color(0xFFF0EBE0),
    outline = WarmGray,
)

@Composable
fun AncientPoetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AncientPoetTypography,
        content = content
    )
}
