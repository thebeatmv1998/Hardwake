package com.wecker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.wecker.app.data.ThemeMode
import kotlin.math.abs

/** Farbpalette im Stil der Zeitrechner-App (TimeCalculator). */
data class AppPalette(
    val themeName: String,
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    /** Eingabefelder / Chips. */
    val input: Color,
    /** Getönte Ergebnisfläche (ResultCard). */
    val result: Color,
    val headerStart: Color,
    val headerEnd: Color
)

/** Index für eine selbst gewählte Farbe statt einer der festen Paletten. */
const val CUSTOM_SCHEME = -1

private val LightBase = Triple(Color(0xFFF2F4F7), Color(0xFFFFFFFF), Color(0xFFEEF1F4))
private val DarkBase = Triple(Color(0xFF121A1E), Color(0xFF1D262B), Color(0xFF2B363D))

private fun palette(
    name: String,
    primary: Long, headerStart: Long, headerEnd: Long, result: Long, dark: Boolean
): AppPalette {
    val (bg, surface, input) = if (dark) DarkBase else LightBase
    return AppPalette(
        themeName = name,
        primary = Color(primary),
        onPrimary = if (dark) Color(0xFF0B1215) else Color.White,
        background = bg,
        surface = surface,
        input = input,
        result = Color(result),
        headerStart = Color(headerStart),
        headerEnd = Color(headerEnd)
    )
}

/** 5 Farbschemata × (Hell, Dunkel) — Farbwerte aus der Zeitrechner-App. */
val AppPalettes: List<Pair<AppPalette, AppPalette>> = listOf(
    palette("Teal",   0xFF00897B, 0xFF26A69A, 0xFF00796B, 0xFFD9EDEA, dark = false) to
        palette("Teal",   0xFF4DB6AC, 0xFF1A7A6D, 0xFF0C473F, 0xFF143936, dark = true),
    palette("Ocean",  0xFF1E88E5, 0xFF42A5F5, 0xFF1565C0, 0xFFDCE9F8, dark = false) to
        palette("Ocean",  0xFF64B5F6, 0xFF15518F, 0xFF0B2E52, 0xFF13293E, dark = true),
    palette("Forest", 0xFF43A047, 0xFF66BB6A, 0xFF2E7D32, 0xFFDFEFDF, dark = false) to
        palette("Forest", 0xFF81C784, 0xFF2C6E35, 0xFF143F1C, 0xFF17301C, dark = true),
    palette("Sunset", 0xFFF57C00, 0xFFFFA726, 0xFFEF6C00, 0xFFFBE6D0, dark = false) to
        palette("Sunset", 0xFFFFB74D, 0xFF9E5A0E, 0xFF5C310A, 0xFF3A2812, dark = true),
    palette("Violet", 0xFF8E24AA, 0xFFAB47BC, 0xFF7B1FA2, 0xFFEFDFF5, dark = false) to
        palette("Violet", 0xFFBA68C8, 0xFF5F2A6C, 0xFF361440, 0xFF2E1737, dark = true)
)

val LocalAppPalette = staticCompositionLocalOf { AppPalettes[0].first }

/**
 * Leitet aus einer Wunschfarbe eine vollständige Palette ab. Helligkeit und
 * Sättigung werden so begrenzt, dass Text auf der Primärfarbe lesbar bleibt.
 */
fun paletteFromSeed(seed: Color, dark: Boolean): AppPalette {
    val (rawHue, rawSat, rawLight) = seed.toHsl()
    val hue = if (rawSat < 0.05f) 210f else rawHue
    val sat = rawSat.coerceIn(0.30f, 1f)

    return if (!dark) {
        val primary = hsl(hue, sat, rawLight.coerceIn(0.32f, 0.52f))
        AppPalette(
            themeName = "Custom",
            primary = primary,
            onPrimary = if (primary.luminance() > 0.28f) Color(0xFF101318) else Color.White,
            background = hsl(hue, 0.10f, 0.965f),
            surface = hsl(hue, 0.05f, 0.995f),
            input = hsl(hue, 0.10f, 0.94f),
            result = hsl(hue, 0.38f, 0.92f),
            headerStart = hsl(hue, sat, (rawLight + 0.12f).coerceIn(0.36f, 0.66f)),
            headerEnd = hsl(hue, sat, (rawLight - 0.18f).coerceIn(0.20f, 0.48f))
        )
    } else {
        val primary = hsl(hue, sat.coerceAtLeast(0.38f), rawLight.coerceIn(0.62f, 0.80f))
        AppPalette(
            themeName = "Custom",
            primary = primary,
            onPrimary = Color(0xFF0B1215),
            background = hsl(hue, 0.18f, 0.09f),
            surface = hsl(hue, 0.15f, 0.14f),
            input = hsl(hue, 0.14f, 0.20f),
            result = hsl(hue, 0.30f, 0.20f),
            headerStart = hsl(hue, sat, 0.30f),
            headerEnd = hsl(hue, sat, 0.17f)
        )
    }
}

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val light = (max + min) / 2f
    val delta = max - min
    val sat = if (delta == 0f) 0f else delta / (1f - abs(2f * light - 1f))
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return Triple((hue + 360f) % 360f, sat.coerceIn(0f, 1f), light)
}

private fun hsl(hue: Float, saturation: Float, lightness: Float): Color {
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val h = ((hue % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs((h % 2f) - 1f))
    val (r, g, b) = when (h.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(r + m, g + m, b + m)
}

/** Liefert die Palette für den gewählten Index (oder die eigene Farbe). */
fun resolvePalette(colorScheme: Int, customPrimary: Int?, dark: Boolean): AppPalette {
    if (colorScheme == CUSTOM_SCHEME && customPrimary != null) {
        return paletteFromSeed(Color(customPrimary), dark)
    }
    val (light, darkPalette) = AppPalettes[colorScheme.coerceIn(AppPalettes.indices)]
    return if (dark) darkPalette else light
}

@Composable
fun WeckerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorScheme: Int = 0,
    customPrimary: Int? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = resolvePalette(colorScheme, customPrimary, darkTheme)

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.input,
            secondaryContainer = palette.result
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            background = palette.background,
            surface = palette.surface,
            surfaceVariant = palette.input,
            secondaryContainer = palette.result
        )
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
