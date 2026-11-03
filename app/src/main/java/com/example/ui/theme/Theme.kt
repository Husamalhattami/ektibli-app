package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.ContrastTheme

data class AccessibilityColors(
    val background: Color,
    val surface: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primary: Color,
    val onPrimary: Color,
    val cardBackground: Color,
    val waveColor: Color
)

private val YellowOnBlackColors = AccessibilityColors(
    background = YellowContrastBg,
    surface = YellowContrastCard,
    surfaceBorder = YellowContrastCardBorder,
    textPrimary = YellowContrastText,
    textSecondary = YellowContrastTextSecondary,
    primary = YellowContrastPrimary,
    onPrimary = YellowContrastOnPrimary,
    cardBackground = YellowContrastCard,
    waveColor = YellowContrastPrimary
)

private val OledDarkColors = AccessibilityColors(
    background = OledDarkBg,
    surface = OledDarkCard,
    surfaceBorder = OledDarkCardBorder,
    textPrimary = OledDarkText,
    textSecondary = OledDarkTextSecondary,
    primary = OledDarkPrimary,
    onPrimary = OledDarkOnPrimary,
    cardBackground = OledDarkCard,
    waveColor = OledDarkPrimary
)

private val LightContrastColors = AccessibilityColors(
    background = LightContrastBg,
    surface = LightContrastCard,
    surfaceBorder = LightContrastCardBorder,
    textPrimary = LightContrastText,
    textSecondary = LightContrastTextSecondary,
    primary = LightContrastPrimary,
    onPrimary = LightContrastOnPrimary,
    cardBackground = LightContrastCard,
    waveColor = LightContrastPrimary
)

private val NavyCyanColors = AccessibilityColors(
    background = NavyBg,
    surface = NavyCard,
    surfaceBorder = NavyCardBorder,
    textPrimary = NavyText,
    textSecondary = NavyTextSecondary,
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    cardBackground = NavyCard,
    waveColor = NavyPrimary
)

private val EmeraldDarkColors = AccessibilityColors(
    background = EmeraldBg,
    surface = EmeraldCard,
    surfaceBorder = EmeraldCardBorder,
    textPrimary = EmeraldText,
    textSecondary = EmeraldTextSecondary,
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    cardBackground = EmeraldCard,
    waveColor = EmeraldPrimary
)

val LocalAppColors = staticCompositionLocalOf { YellowOnBlackColors }

object AppTheme {
    val colors: AccessibilityColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun EktibliTheme(
    contrastTheme: ContrastTheme = ContrastTheme.YELLOW_ON_BLACK,
    content: @Composable () -> Unit
) {
    val accessColors = when (contrastTheme) {
        ContrastTheme.YELLOW_ON_BLACK -> YellowOnBlackColors
        ContrastTheme.DARK_OLED -> OledDarkColors
        ContrastTheme.LIGHT_HIGH_CONTRAST -> LightContrastColors
        ContrastTheme.NAVY_CYAN -> NavyCyanColors
        ContrastTheme.EMERALD_DARK -> EmeraldDarkColors
    }

    val isDark = contrastTheme != ContrastTheme.LIGHT_HIGH_CONTRAST
    val m3ColorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = accessColors.primary,
            onPrimary = accessColors.onPrimary,
            background = accessColors.background,
            onBackground = accessColors.textPrimary,
            surface = accessColors.surface,
            onSurface = accessColors.textPrimary,
            surfaceVariant = accessColors.cardBackground,
            onSurfaceVariant = accessColors.textSecondary,
            outline = accessColors.surfaceBorder
        )
    } else {
        lightColorScheme(
            primary = accessColors.primary,
            onPrimary = accessColors.onPrimary,
            background = accessColors.background,
            onBackground = accessColors.textPrimary,
            surface = accessColors.surface,
            onSurface = accessColors.textPrimary,
            surfaceVariant = accessColors.cardBackground,
            onSurfaceVariant = accessColors.textSecondary,
            outline = accessColors.surfaceBorder
        )
    }

    CompositionLocalProvider(LocalAppColors provides accessColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
