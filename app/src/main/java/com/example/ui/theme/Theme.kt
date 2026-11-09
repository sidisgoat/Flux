package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class FluxVisualTokens(
    val themeMode: FluxThemeMode = FluxThemeMode.DARK,
    val accent: FluxAccentPalette = FluxAccentPalette.ELECTRIC_BLUE,
    val surfaceOpacity: Float = 0.90f,
    val cornerRadiusDp: Int = 18,
    val showThinBorders: Boolean = true,
    val animationIntensity: Float = 1.0f,
    val isAmoled: Boolean = false
)

val LocalFluxVisuals = staticCompositionLocalOf { FluxVisualTokens() }

@Composable
fun FluxTheme(
    themeMode: FluxThemeMode = FluxThemeMode.DARK,
    accent: FluxAccentPalette = FluxAccentPalette.ELECTRIC_BLUE,
    surfaceOpacity: Float = 0.90f,
    cornerRadiusDp: Int = 18,
    showThinBorders: Boolean = true,
    animationIntensity: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        FluxThemeMode.DARK, FluxThemeMode.AMOLED -> true
        FluxThemeMode.LIGHT -> false
        FluxThemeMode.SYSTEM -> systemDark
    }
    val isAmoled = themeMode == FluxThemeMode.AMOLED

    val colorScheme = if (useDark) {
        val bg = if (isAmoled) AmoledBlack else CharcoalDeep
        val surface = if (isAmoled) AmoledSurface else CharcoalSurface
        val elevated = if (isAmoled) AmoledElevated else CharcoalElevated
        darkColorScheme(
            primary = accent.primary,
            onPrimary = Color(0xFF061018),
            primaryContainer = accent.containerDark,
            onPrimaryContainer = TextWarmWhite,
            secondary = accent.secondary,
            onSecondary = Color(0xFF061018),
            secondaryContainer = elevated,
            onSecondaryContainer = TextWarmWhite,
            tertiary = Color(0xFF38BDF8),
            background = bg,
            onBackground = TextWarmWhite,
            surface = surface,
            onSurface = TextWarmWhite,
            surfaceVariant = elevated,
            onSurfaceVariant = TextCoolGrey,
            outline = CharcoalBorder,
            outlineVariant = CharcoalBorder.copy(alpha = 0.55f)
        )
    } else {
        lightColorScheme(
            primary = if (accent == FluxAccentPalette.MONOCHROME_SLATE) Color(0xFF1E293B) else accent.primary,
            onPrimary = Color.White,
            primaryContainer = accent.containerLight,
            onPrimaryContainer = TextDarkInk,
            secondary = accent.secondary,
            onSecondary = Color.White,
            secondaryContainer = LightElevated,
            onSecondaryContainer = TextDarkInk,
            tertiary = Color(0xFF0284C7),
            background = LightPaper,
            onBackground = TextDarkInk,
            surface = LightSurface,
            onSurface = TextDarkInk,
            surfaceVariant = LightElevated,
            onSurfaceVariant = TextDarkMuted,
            outline = LightBorder,
            outlineVariant = LightBorder.copy(alpha = 0.65f)
        )
    }

    val radius: Dp = cornerRadiusDp.coerceIn(0, 32).dp
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((cornerRadiusDp / 3).coerceAtLeast(2).dp),
        small = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(4).dp),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape((cornerRadiusDp + 6).coerceAtMost(36).dp),
        extraLarge = RoundedCornerShape((cornerRadiusDp + 12).coerceAtMost(42).dp)
    )

    val visualTokens = FluxVisualTokens(
        themeMode = themeMode,
        accent = accent,
        surfaceOpacity = surfaceOpacity.coerceIn(0.45f, 1.0f),
        cornerRadiusDp = cornerRadiusDp,
        showThinBorders = showThinBorders,
        animationIntensity = animationIntensity,
        isAmoled = isAmoled
    )

    CompositionLocalProvider(LocalFluxVisuals provides visualTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FluxTypography,
            shapes = shapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    FluxTheme(
        themeMode = if (darkTheme) FluxThemeMode.DARK else FluxThemeMode.LIGHT,
        content = content
    )
}
