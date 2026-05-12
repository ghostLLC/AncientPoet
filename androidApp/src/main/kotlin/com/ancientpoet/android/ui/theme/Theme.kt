package com.ancientpoet.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================
// AncientPoet Design System — Color Palette
// Matches DESIGN/DESIGN.md YAML specification
// ============================================

// Material 3 Color Roles
val Primary = Color(0xFFa73428); val OnPrimary = Color(0xFFfffdff)
val PrimaryContainer = Color(0xFFc84c3d); val OnPrimaryContainer = Color(0xFFfffdff)
val InversePrimary = Color(0xFFffb4a9)

val Secondary = Color(0xFF775a19); val OnSecondary = Color(0xFFffffff)
val SecondaryContainer = Color(0xFFfdd588); val OnSecondaryContainer = Color(0xFF775b1a)

val Tertiary = Color(0xFF426453); val OnTertiary = Color(0xFFffffff)
val TertiaryContainer = Color(0xFF5a7d6b); val OnTertiaryContainer = Color(0xFFfbfffa)

val ErrorColor = Color(0xFFba1a1a); val OnErrorColor = Color(0xFFffffff)
val ErrorContainerColor = Color(0xFFffdad6); val OnErrorContainerColor = Color(0xFF93000a)

val BgColor = Color(0xFFfcf8ff); val OnBgColor = Color(0xFF1a1a2e)
val SurfaceColor = Color(0xFFfcf8ff); val OnSurfaceColor = Color(0xFF1a1a2e)
val SurfaceVariantColor = Color(0xFFe2e0fc); val OnSurfaceVariantColor = Color(0xFF58413e)
val OutlineColor = Color(0xFF8b716d); val OutlineVariantColor = Color(0xFFdfbfba)

// Semantic colors — traditional Chinese pigments
val RicePaper = Color(0xFFF5F0E8)
val InkBlack = Color(0xFF1A1A2E)
val VermilionRed = Color(0xFFC84C3D)
val ImperialGold = Color(0xFFC8A45C)
val JadeGreen = Color(0xFF6B8E7B)
val SkyBlue = Color(0xFF7BA7BC)
val WarmGray = Color(0xFF8B8178)

private val LightScheme = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary, onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer, onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary, onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer, onTertiaryContainer = OnTertiaryContainer,
    error = ErrorColor, onError = OnErrorColor,
    errorContainer = ErrorContainerColor, onErrorContainer = OnErrorContainerColor,
    background = BgColor, onBackground = OnBgColor,
    surface = SurfaceColor, onSurface = OnSurfaceColor,
    surfaceVariant = SurfaceVariantColor, onSurfaceVariant = OnSurfaceVariantColor,
    outline = OutlineColor, outlineVariant = OutlineVariantColor,
)

@Composable
fun AncientPoetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = AncientPoetTypography,
        content = content,
    )
}
