package com.ancientpoet.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography matching DESIGN/DESIGN.md YAML spec
// headline-display: 48sp Serif 700 / headline-poet: 28sp Serif 600
// body-letter: 18sp Serif 400 / body-standard: 16sp Serif 400
// label-ui: 14sp Sans 500

val SerifFont = FontFamily.Serif
val SansFont = FontFamily.Default

val AncientPoetTypography = Typography(
    displayLarge = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 64.sp),
    headlineMedium = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = SansFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 32.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 28.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = SerifFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(fontFamily = SansFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = SansFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
