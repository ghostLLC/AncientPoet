package com.ancientpoet.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Traditional pigments are accents; content and controls use semantic Material roles.
val RicePaper = Color(0xFFF7F3E9)
val InkBlack = Color(0xFF29251F)
val VermilionRed = Color(0xFF9F3D31)
val ImperialGold = Color(0xFF775A25)
val JadeGreen = Color(0xFF436653)
val SkyBlue = Color(0xFF365F73)
val WarmGray = Color(0xFF655D54)
val Primary = VermilionRed
val OnPrimary = Color.White
val PrimaryContainer = Color(0xFFF3DBD3)
val OnPrimaryContainer = Color(0xFF4F201B)
val InversePrimary = Color(0xFFF3B2A8)
val Secondary = ImperialGold
val OnSecondary = Color.White
val SecondaryContainer = Color(0xFFECE1C6)
val OnSecondaryContainer = Color(0xFF473819)
val Tertiary = JadeGreen
val OnTertiary = Color.White
val TertiaryContainer = Color(0xFFD7E5DA)
val OnTertiaryContainer = Color(0xFF243F30)
val ErrorColor = Color(0xFFB3261E)
val OnErrorColor = Color.White
val ErrorContainerColor = Color(0xFFF9DEDC)
val OnErrorContainerColor = Color(0xFF601410)
val BgColor = RicePaper
val OnBgColor = InkBlack
val SurfaceColor = Color(0xFFFCF9F2)
val OnSurfaceColor = InkBlack
val SurfaceVariantColor = Color(0xFFECE7DA)
val OnSurfaceVariantColor = WarmGray
val OutlineColor = Color(0xFF7A7063)
val OutlineVariantColor = Color(0xFFD7CEBF)

private val LightScheme = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary, primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary, onSecondary = OnSecondary, secondaryContainer = SecondaryContainer, onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary, onTertiary = OnTertiary, tertiaryContainer = TertiaryContainer, onTertiaryContainer = OnTertiaryContainer,
    background = RicePaper, onBackground = InkBlack, surface = SurfaceColor, onSurface = InkBlack,
    surfaceVariant = SurfaceVariantColor, onSurfaceVariant = WarmGray, outline = OutlineColor, outlineVariant = OutlineVariantColor,
    surfaceContainerLowest = SurfaceColor, surfaceContainerLow = RicePaper, surfaceContainer = Color(0xFFF0EADF),
    surfaceContainerHigh = Color(0xFFEAE3D6), surfaceContainerHighest = Color(0xFFE3DCCC)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF2AAA0), onPrimary = Color(0xFF4C211C), primaryContainer = Color(0xFF68352D), onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFE0C58E), onSecondary = Color(0xFF3A2F18), secondaryContainer = Color(0xFF4D4029), onSecondaryContainer = Color(0xFFF2E1BE),
    tertiary = Color(0xFFAACDB6), onTertiary = Color(0xFF1A3728),
    background = Color(0xFF1C1C19), onBackground = Color(0xFFEDE7DB), surface = Color(0xFF22231F), onSurface = Color(0xFFEDE7DB),
    surfaceVariant = Color(0xFF35372F), onSurfaceVariant = Color(0xFFC7C1B4), outline = Color(0xFF938D80), outlineVariant = Color(0xFF4E4D43),
    surfaceContainerLowest = Color(0xFF171814), surfaceContainerLow = Color(0xFF22231F), surfaceContainer = Color(0xFF282922),
    surfaceContainerHigh = Color(0xFF30312A), surfaceContainerHighest = Color(0xFF393A31)
)

@Composable
fun AncientPoetTheme(darkTheme: Boolean = isSystemInDarkTheme(), textScale: Float = 1f, content: @Composable () -> Unit) {
    val type = AncientPoetTypography
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = type.copy(
            bodyLarge = type.bodyLarge.copy(fontSize = type.bodyLarge.fontSize * textScale, lineHeight = type.bodyLarge.lineHeight * textScale),
            bodyMedium = type.bodyMedium.copy(fontSize = type.bodyMedium.fontSize * textScale, lineHeight = type.bodyMedium.lineHeight * textScale),
            bodySmall = type.bodySmall.copy(fontSize = type.bodySmall.fontSize * textScale, lineHeight = type.bodySmall.lineHeight * textScale)
        ),
        shapes = Shapes(small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(16.dp)),
        content = content
    )
}
