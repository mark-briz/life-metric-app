package com.markbriz.lifemetric.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val id: String,
    val label: String,
    val description: String,
    val preview: List<Color>,
    val lightStatusBar: Boolean,
    val brand: Color,
    val brandDark: Color,
    val brandSoft: Color,
    val accent: Color,
    val accentSoft: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val border: Color,
    val danger: Color,
    val success: Color,
    val overlay: Color,
    val gradientTop: Color,
    val gradientMid: Color,
    val gradientBottom: Color,
)

private fun theme(
    id: String,
    label: String,
    description: String,
    preview: List<Long>,
    lightStatusBar: Boolean,
    brand: Long,
    brandDark: Long,
    brandSoft: Long,
    accent: Long,
    accentSoft: Long,
    ink: Long,
    inkMuted: Long,
    inkFaint: Long,
    surface: Long,
    surfaceMuted: Long,
    border: Long,
    danger: Long,
    success: Long,
    overlay: Long,
    gradientTop: Long,
    gradientMid: Long,
    gradientBottom: Long,
) = AppColors(
    id, label, description, preview.map { Color(it) }, lightStatusBar,
    Color(brand), Color(brandDark), Color(brandSoft), Color(accent), Color(accentSoft),
    Color(ink), Color(inkMuted), Color(inkFaint), Color(surface), Color(surfaceMuted),
    Color(border), Color(danger), Color(success), Color(overlay),
    Color(gradientTop), Color(gradientMid), Color(gradientBottom),
)

val MintTheme = theme(
    "mint", "Mint", "Teal and coral on soft mint gradients.",
    listOf(0xFFD1FAE5, 0xFFFEF3C7, 0xFFFFEDD5), true,
    0xFF0D9488, 0xFF0F766E, 0xFFCCFBF1, 0xFFF97316, 0xFFFFEDD5,
    0xFF0F172A, 0xFF64748B, 0xFF94A3B8, 0xFFFFFFFF, 0xFFF8FAFC, 0xFFE2E8F0,
    0xFFDC2626, 0xFF059669, 0x730F172A, 0xFFD1FAE5, 0xFFFEF3C7, 0xFFFFEDD5,
)

val OceanTheme = theme(
    "ocean", "Ocean", "Cool sky blues with deep slate text.",
    listOf(0xFFE0F2FE, 0xFFDBEAFE, 0xFFE0E7FF), true,
    0xFF0284C7, 0xFF0369A1, 0xFFE0F2FE, 0xFF6366F1, 0xFFE0E7FF,
    0xFF0F172A, 0xFF475569, 0xFF94A3B8, 0xFFFFFFFF, 0xFFF8FAFC, 0xFFCBD5E1,
    0xFFDC2626, 0xFF059669, 0x730F172A, 0xFFE0F2FE, 0xFFDBEAFE, 0xFFE0E7FF,
)

val SunsetTheme = theme(
    "sunset", "Sunset", "Warm rose, peach, and amber tones.",
    listOf(0xFFFCE7F3, 0xFFFFEDD5, 0xFFFEF3C7), true,
    0xFFDB2777, 0xFFBE185D, 0xFFFCE7F3, 0xFFEA580C, 0xFFFFEDD5,
    0xFF431407, 0xFF9A3412, 0xFFC2410C, 0xFFFFFFFF, 0xFFFFF7ED, 0xFFFED7AA,
    0xFFDC2626, 0xFF059669, 0x73431407, 0xFFFCE7F3, 0xFFFFEDD5, 0xFFFEF3C7,
)

val DarkTheme = theme(
    "dark", "Dark", "Midnight surfaces with bright teal accents.",
    listOf(0xFF0F172A, 0xFF1E293B, 0xFF134E4A), false,
    0xFF2DD4BF, 0xFF14B8A6, 0xFF134E4A, 0xFFFB923C, 0xFF431407,
    0xFFF8FAFC, 0xFF94A3B8, 0xFF64748B, 0xFF1E293B, 0xFF0F172A, 0xFF334155,
    0xFFF87171, 0xFF34D399, 0x8C000000, 0xFF0F172A, 0xFF1E293B, 0xFF134E4A,
)

val AppThemes = listOf(MintTheme, OceanTheme, SunsetTheme, DarkTheme)

fun themeById(id: String): AppColors = AppThemes.find { it.id == id } ?: MintTheme

fun AppColors.materialColorScheme(): ColorScheme {
    val onBrand = if (lightStatusBar) Color.White else Color(0xFF042F2E)
    val schemeColors: ColorScheme.() -> ColorScheme = {
        copy(
            primary = brand,
            onPrimary = onBrand,
            primaryContainer = brandSoft,
            onPrimaryContainer = if (lightStatusBar) brandDark else ink,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = accentSoft,
            onSecondaryContainer = if (lightStatusBar) accent else ink,
            background = surfaceMuted,
            onBackground = ink,
            surface = surface,
            onSurface = ink,
            surfaceVariant = surfaceMuted,
            onSurfaceVariant = inkMuted,
            outline = border,
            outlineVariant = border,
            error = danger,
            onError = if (lightStatusBar) Color.White else Color(0xFF1E293B),
            errorContainer = danger.copy(alpha = 0.16f),
            onErrorContainer = danger,
        )
    }
    return if (lightStatusBar) {
        lightColorScheme().schemeColors()
    } else {
        darkColorScheme().schemeColors()
    }
}

val LocalAppColors = staticCompositionLocalOf { MintTheme }
