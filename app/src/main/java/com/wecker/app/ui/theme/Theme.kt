package com.wecker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.wecker.app.data.ThemeMode

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

@Composable
fun WeckerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorScheme: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val (light, dark) = AppPalettes[colorScheme.coerceIn(AppPalettes.indices)]
    val palette = if (darkTheme) dark else light

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
