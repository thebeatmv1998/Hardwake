package com.wecker.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wecker.app.data.SettingsRepository
import com.wecker.app.data.ThemeMode
import com.wecker.app.i18n.AppLanguage
import com.wecker.app.i18n.LocalStrings
import com.wecker.app.i18n.Strings
import com.wecker.app.ui.theme.AppPalettes
import com.wecker.app.ui.theme.CUSTOM_SCHEME
import com.wecker.app.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SOURCE_URL = "https://github.com/thebeatmv1998/Hardwake"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val language by settings.language.collectAsState(initial = AppLanguage.SYSTEM)
    val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val colorScheme by settings.colorScheme.collectAsState(initial = 0)
    val customPrimary by settings.customPrimary.collectAsState(initial = null)
    val snoozeMinutes by settings.snoozeMinutes.collectAsState(initial = 10)
    val vibrate by settings.vibrate.collectAsState(initial = true)

    var showColorPicker by remember { mutableStateOf(false) }
    var pickerStart by remember { mutableStateOf(Color(0xFF00897B)) }
    val activePalette = LocalAppPalette.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(
                title = s.settingsTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.back,
                            tint = Color.White
                        )
                    }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Section(s.languageSection) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = language == option,
                                onClick = { scope.launch { settings.setLanguage(option) } },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = AppLanguage.entries.size
                                )
                            ) { Text(languageLabel(option, s)) }
                        }
                    }
                }

                Section(s.appearanceSection) {
                    Text(s.themeLabel, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { scope.launch { settings.setThemeMode(mode) } },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = ThemeMode.entries.size
                                )
                            ) { Text(themeLabel(mode, s)) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(s.paletteLabel, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        s.paletteNames.forEachIndexed { index, name ->
                            val (light, _) = AppPalettes[index]
                            FilterChip(
                                selected = colorScheme == index,
                                onClick = { scope.launch { settings.setColorScheme(index) } },
                                label = { Text(name) },
                                leadingIcon = {
                                    Swatch(
                                        brush = Brush.horizontalGradient(
                                            listOf(light.headerStart, light.headerEnd)
                                        )
                                    )
                                }
                            )
                        }
                        FilterChip(
                            selected = colorScheme == CUSTOM_SCHEME,
                            onClick = {
                                pickerStart = customPrimary
                                    ?.let { Color(it) }
                                    ?: activePalette.primary
                                showColorPicker = true
                            },
                            label = { Text(s.paletteCustom) },
                            leadingIcon = {
                                if (customPrimary != null) {
                                    Swatch(brush = Brush.linearGradient(
                                        listOf(Color(customPrimary!!), Color(customPrimary!!))
                                    ))
                                } else {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Section(s.alarmsSection) {
                    var snooze by remember(snoozeMinutes) { mutableIntStateOf(snoozeMinutes) }
                    Text(
                        "${s.snoozeLabel}: ${s.minutesShort(snooze)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = snooze.toFloat(),
                        onValueChange = { snooze = (it / 5f).roundToInt() * 5 },
                        onValueChangeFinished = {
                            scope.launch { settings.setSnoozeMinutes(snooze) }
                        },
                        valueRange = 5f..30f,
                        steps = 4
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.vibrationLabel, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                s.vibrationHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { scope.launch { settings.setVibrate(it) } }
                        )
                    }
                }

                Section(s.aboutSection) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(s.versionLabel, modifier = Modifier.weight(1f))
                        Text(appVersion(context), color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.privacyNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
                            }
                        }
                    ) { Text(s.sourceCode) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initial = pickerStart,
            onDismiss = { showColorPicker = false },
            onConfirm = { color ->
                scope.launch {
                    settings.setCustomPrimary(color.toArgb())
                    settings.setColorScheme(CUSTOM_SCHEME)
                }
                showColorPicker = false
            }
        )
    }
}

private fun languageLabel(language: AppLanguage, s: Strings): String = when (language) {
    AppLanguage.SYSTEM -> s.languageSystem
    AppLanguage.GERMAN -> s.languageGerman
    AppLanguage.ENGLISH -> s.languageEnglish
}

private fun themeLabel(mode: ThemeMode, s: Strings): String = when (mode) {
    ThemeMode.SYSTEM -> s.themeSystem
    ThemeMode.LIGHT -> s.themeLight
    ThemeMode.DARK -> s.themeDark
}

@Composable
private fun Swatch(brush: Brush) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(brush)
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun appVersion(context: Context): String = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: ""

// ---------------------------------------------------------------------------
// Farbwähler
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val s = LocalStrings.current
    val (initialHue, initialSat, initialValue) = remember(initial) { initial.toHsv() }
    var hue by remember { mutableFloatStateOf(initialHue) }
    var sat by remember { mutableFloatStateOf(initialSat) }
    var value by remember { mutableFloatStateOf(initialValue) }
    val color = Color.hsv(hue, sat, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.colorDialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(s.hexLabel, style = MaterialTheme.typography.labelSmall)
                        Text(hexOf(color), style = MaterialTheme.typography.titleMedium)
                    }
                }

                SaturationValueBox(
                    hue = hue,
                    sat = sat,
                    value = value,
                    onChange = { newSat, newValue -> sat = newSat; value = newValue }
                )

                Text(s.hueLabel, style = MaterialTheme.typography.labelMedium)
                HueSlider(hue = hue, onChange = { hue = it })

                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Color(0xFF00897B), Color(0xFF1E88E5), Color(0xFF43A047),
                        Color(0xFFF57C00), Color(0xFF8E24AA), Color(0xFFE53935)
                    ).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(preset)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    val (h, sv, v) = preset.toHsv()
                                    hue = h
                                    sat = sv
                                    value = v
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) { Text(s.apply) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        }
    )
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (Float, Float) -> Unit
) {
    val hueColor = Color.hsv(hue, 1f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange(
                        (down.position.x / size.width).coerceIn(0f, 1f),
                        (1f - down.position.y / size.height).coerceIn(0f, 1f)
                    )
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        onChange(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        )
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(sat * size.width, (1f - value) * size.height)
            drawCircle(
                Color.Black.copy(alpha = 0.35f),
                radius = 11.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
            )
            drawCircle(
                Color.White,
                radius = 9.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit) {
    val hues = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.horizontalGradient(hues))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onChange((down.position.x / size.width).coerceIn(0f, 1f) * 360f)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        onChange((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val x = (hue / 360f) * size.width
            drawCircle(
                Color.White,
                radius = 9.dp.toPx(),
                center = Offset(x, size.height / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx())
            )
        }
    }
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    val sat = if (max == 0f) 0f else delta / max
    return Triple((hue + 360f) % 360f, sat.coerceIn(0f, 1f), max.coerceIn(0f, 1f))
}

private fun hexOf(color: Color): String = "#%06X".format(0xFFFFFF and color.toArgb())
