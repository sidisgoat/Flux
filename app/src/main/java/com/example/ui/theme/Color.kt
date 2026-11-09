package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Aesthetic: Deep charcoal, near-black, pure AMOLED black, and warm editorial light
val CharcoalDeep = Color(0xFF0B0E14)
val CharcoalSurface = Color(0xFF121721)
val CharcoalElevated = Color(0xFF19202E)
val CharcoalBorder = Color(0xFF263042)

val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF090C10)
val AmoledElevated = Color(0xFF11161F)

val LightPaper = Color(0xFFF6F5F0)
val LightSurface = Color(0xFFFFFFFF)
val LightElevated = Color(0xFFECEAE2)
val LightBorder = Color(0xFFD8D5CA)

val TextWarmWhite = Color(0xFFF4F4F6)
val TextCoolGrey = Color(0xFF94A3B8)
val TextMutedGrey = Color(0xFF64748B)
val TextDarkInk = Color(0xFF111827)
val TextDarkMuted = Color(0xFF4B5563)

enum class FluxAccentPalette(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val containerDark: Color,
    val containerLight: Color
) {
    ELECTRIC_BLUE(
        id = "electric_blue",
        label = "Electric Blue",
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF818CF8),
        containerDark = Color(0xFF0C2A3E),
        containerLight = Color(0xFFE0F2FE)
    ),
    AMBER_PULSE(
        id = "amber_pulse",
        label = "Warm Amber",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFFB923C),
        containerDark = Color(0xFF332008),
        containerLight = Color(0xFFFEF3C7)
    ),
    EMERALD_SIGNAL(
        id = "emerald_signal",
        label = "Signal Emerald",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF34D399),
        containerDark = Color(0xFF082E24),
        containerLight = Color(0xFFD1FAE5)
    ),
    CRIMSON_ROSE(
        id = "crimson_rose",
        label = "Editorial Rose",
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFFB7185),
        containerDark = Color(0xFF36101B),
        containerLight = Color(0xFFFFE4E6)
    ),
    VIOLET_WAVE(
        id = "violet_wave",
        label = "Cobalt Violet",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFC084FC),
        containerDark = Color(0xFF29123D),
        containerLight = Color(0xFFF3E8FF)
    ),
    MONOCHROME_SLATE(
        id = "monochrome_slate",
        label = "Monochrome",
        primary = Color(0xFFE2E8F0),
        secondary = Color(0xFF94A3B8),
        containerDark = Color(0xFF1E293B),
        containerLight = Color(0xFFE2E8F0)
    );

    companion object {
        fun fromId(id: String): FluxAccentPalette =
            entries.firstOrNull { it.id == id } ?: ELECTRIC_BLUE
    }
}

enum class FluxThemeMode(val id: String, val label: String) {
    DARK("dark", "Charcoal Dark"),
    AMOLED("amoled", "AMOLED Pure Black"),
    LIGHT("light", "Editorial Light"),
    SYSTEM("system", "Follow System");

    companion object {
        fun fromId(id: String): FluxThemeMode =
            entries.firstOrNull { it.id == id } ?: DARK
    }
}
