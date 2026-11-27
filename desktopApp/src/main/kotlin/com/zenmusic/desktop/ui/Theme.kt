@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.zenmusic.desktop.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.FontHinting
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ZenPurple = Color(0xFFBB86FC)
val ZenPurpleDeep = Color(0xFF7C4DFF)
val ZenBackground = Color(0xFF050507)
val ZenSurface = Color(0xFF101014)
val ZenSurfaceVariant = Color(0xFF1B1B21)
val ZenSurfaceElevated = Color(0xFF22222A)
val ZenText = Color(0xFFF5F3F7)
val ZenTextSecondary = Color(0xFFB6B3C0)
val ZenDivider = Color(0x1FFFFFFF)
val ZenError = Color(0xFFFF5C6C)

private val ZenColors = darkColorScheme(
    primary = ZenPurple,
    onPrimary = Color(0xFF1A0024),
    primaryContainer = Color(0xFF2E1B44),
    onPrimaryContainer = Color(0xFFEAD7FF),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color(0xFF00201C),
    background = ZenBackground,
    onBackground = ZenText,
    surface = ZenSurface,
    onSurface = ZenText,
    surfaceVariant = ZenSurfaceVariant,
    onSurfaceVariant = ZenTextSecondary,
    surfaceContainer = Color(0xFF16161B),
    surfaceContainerHigh = Color(0xFF1D1D23),
    surfaceContainerHighest = Color(0xFF24242C),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    error = ZenError,
    onError = Color(0xFF2A0004),
)

private val ZenFontRasterization = FontRasterizationSettings(
    // Субпиксельное сглаживание + хинтинг + отключённое дробное
    // позиционирование дают заметно более чёткий текст на Windows.
    smoothing = FontSmoothing.AntiAlias,
    hinting = FontHinting.Slight,
    subpixelPositioning = false,
    autoHintingForced = false,
)

private fun TextStyle.zenRaster(): TextStyle = merge(
    ParagraphStyle(
        platformStyle = PlatformParagraphStyle(
            fontRasterizationSettings = ZenFontRasterization,
        ),
    ),
)

private fun Typography.withZenRaster(): Typography = Typography(
    displayLarge = displayLarge.zenRaster(),
    displayMedium = displayMedium.zenRaster(),
    displaySmall = displaySmall.zenRaster(),
    headlineLarge = headlineLarge.zenRaster(),
    headlineMedium = headlineMedium.zenRaster(),
    headlineSmall = headlineSmall.zenRaster(),
    titleLarge = titleLarge.zenRaster(),
    titleMedium = titleMedium.zenRaster(),
    titleSmall = titleSmall.zenRaster(),
    bodyLarge = bodyLarge.zenRaster(),
    bodyMedium = bodyMedium.zenRaster(),
    bodySmall = bodySmall.zenRaster(),
    labelLarge = labelLarge.zenRaster(),
    labelMedium = labelMedium.zenRaster(),
    labelSmall = labelSmall.zenRaster(),
)

private val ZenTypography = Typography(
    // Размеры подобраны так, чтобы при масштабе Windows 125% они попадали
    // в целые пиксели и текст выглядел заметно чётче.
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.4.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.4.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.6.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.4.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.2.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 14.4.sp,
        lineHeight = 21.6.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 12.8.sp,
        lineHeight = 19.2.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.8.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.8.sp,
        lineHeight = 17.6.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.2.sp,
        lineHeight = 16.sp,
    ),
).withZenRaster()

@Composable
fun ZenMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZenColors,
        typography = ZenTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides ZenTypography.bodyLarge,
            content = content,
        )
    }
}
