package com.wecker.app.ui

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wecker.app.alarm.AlarmScheduler
import com.wecker.app.data.Alarm
import com.wecker.app.data.ChallengeType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Wiederholungsart eines Weckers. */
private enum class RepeatMode(val label: String) {
    ONCE("Einmalig"),
    DATE("Datum"),
    WEEKLY("Wochentage")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    learnedBarcode: String?,
    onBarcodeConsumed: () -> Unit,
    onLearnBarcode: () -> Unit,
    onBack: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var initialHour by remember { mutableIntStateOf(7) }
    var initialMinute by remember { mutableIntStateOf(0) }
    var repeatMode by remember { mutableStateOf(RepeatMode.ONCE) }
    var daysMask by remember { mutableIntStateOf(0) }
    var dateEpochDay by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    var challengeType by remember { mutableStateOf(ChallengeType.NONE) }
    var mathCount by remember { mutableIntStateOf(3) }
    var mathDifficulty by remember { mutableIntStateOf(1) }
    var barcodeValue by remember { mutableStateOf<String?>(null) }
    var stepGoal by remember { mutableIntStateOf(30) }
    var wakeCheckMinutes by remember { mutableIntStateOf(0) }
    var ringtoneUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(alarmId) {
        viewModel.load(alarmId)?.let { alarm ->
            initialHour = alarm.hour
            initialMinute = alarm.minute
            repeatMode = when {
                alarm.dateEpochDay != null -> RepeatMode.DATE
                alarm.daysMask != 0 -> RepeatMode.WEEKLY
                else -> RepeatMode.ONCE
            }
            daysMask = alarm.daysMask
            dateEpochDay = alarm.dateEpochDay
            label = alarm.label
            challengeType = alarm.challengeType
            mathCount = alarm.mathCount
            mathDifficulty = alarm.mathDifficulty
            barcodeValue = alarm.barcodeValue
            stepGoal = alarm.stepGoal
            wakeCheckMinutes = alarm.wakeCheckMinutes
            ringtoneUri = alarm.ringtoneUri
        }
        // Race-Fix: Falls ein Barcode schon eingescannt wurde (z. B. weil die Activity
        // während des Scans neu erstellt wurde), hat er Vorrang vor den DB-Werten.
        learnedBarcode?.let {
            barcodeValue = it
            challengeType = ChallengeType.BARCODE
            onBarcodeConsumed()
        }
        loaded = true
    }

    // Vom Barcode-Learn-Screen zurückgekommen.
    LaunchedEffect(learnedBarcode) {
        if (learnedBarcode != null) {
            barcodeValue = learnedBarcode
            challengeType = ChallengeType.BARCODE
            onBarcodeConsumed()
        }
    }

    if (!loaded) return

    val timeState = rememberTimePickerState(initialHour, initialMinute, is24Hour = true)
    val barcodeMissing = challengeType == ChallengeType.BARCODE && barcodeValue == null

    // Live-Vorschau: Wann klingelt dieser Wecker mit den aktuell gewählten Einstellungen?
    val previewNext = remember(
        timeState.hour, timeState.minute, repeatMode, daysMask, dateEpochDay
    ) {
        AlarmScheduler.nextTriggerMillis(
            Alarm(
                id = 0,
                hour = timeState.hour,
                minute = timeState.minute,
                daysMask = if (repeatMode == RepeatMode.WEEKLY) daysMask else 0,
                dateEpochDay = if (repeatMode == RepeatMode.DATE) dateEpochDay else null
            )
        )
    }
    val dateInPast = repeatMode == RepeatMode.DATE && previewNext == null
    val saveEnabled = !barcodeMissing && !dateInPast

    // Klingelton-Auswahl über den System-Picker (Töne + eigene Musik).
    val context = LocalContext.current
    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        // null bedeutet im Picker „Stumm" – das ignorieren wir, URI bleibt wie sie ist.
        if (result.data != null) ringtoneUri = uri?.toString()
    }
    val ringtoneTitle = remember(ringtoneUri) {
        ringtoneUri
            ?.let { runCatching { RingtoneManager.getRingtone(context, Uri.parse(it)).getTitle(context) }.getOrNull() }
            ?: "Standard-Alarmton"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(
                title = if (alarmId == 0L) "Neuer Wecker" else "Wecker bearbeiten",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = saveEnabled,
                        onClick = {
                            val alarm = Alarm(
                                id = alarmId,
                                hour = timeState.hour,
                                minute = timeState.minute,
                                daysMask = if (repeatMode == RepeatMode.WEEKLY) daysMask else 0,
                                dateEpochDay = if (repeatMode == RepeatMode.DATE) dateEpochDay else null,
                                label = label.trim(),
                                enabled = true,
                                challengeType = challengeType,
                                mathCount = mathCount,
                                mathDifficulty = mathDifficulty,
                                barcodeValue = if (challengeType == ChallengeType.BARCODE) barcodeValue else null,
                                stepGoal = stepGoal,
                                wakeCheckMinutes = wakeCheckMinutes,
                                ringtoneUri = ringtoneUri
                            )
                            scope.launch {
                                viewModel.save(alarm)
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Speichern",
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
                // Weckzeit mit Live-Zusammenfassung
                SectionCard {
                    TimePicker(state = timeState)
                    Spacer(Modifier.height(8.dp))
                    val summary = when {
                        dateInPast -> "Dieses Datum liegt in der Vergangenheit"
                        previewNext != null -> "Klingelt ${triggerDayLabel(previewNext)} um " +
                            "%02d:%02d · %s".format(
                                timeState.hour, timeState.minute,
                                relativeLabel((previewNext - System.currentTimeMillis()).coerceAtLeast(0) / 1000)
                            )
                        else -> ""
                    }
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (dateInPast) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }

                // Wiederholung
                SectionCard(title = "Wiederholung", contentAlignment = Alignment.Start) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        RepeatMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = repeatMode == mode,
                                onClick = {
                                    repeatMode = mode
                                    // Smarte Defaults: Datum-Modus startet mit morgen,
                                    // Wochentage-Modus mit Mo–Fr.
                                    if (mode == RepeatMode.DATE && dateEpochDay == null) {
                                        dateEpochDay = LocalDate.now().plusDays(1).toEpochDay()
                                    }
                                    if (mode == RepeatMode.WEEKLY && daysMask == 0) {
                                        daysMask = 0b0011111
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = RepeatMode.entries.size
                                )
                            ) { Text(mode.label) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AnimatedContent(
                        targetState = repeatMode,
                        transitionSpec = {
                            (fadeIn(tween(220)) + expandVertically(tween(220)))
                                .togetherWith(fadeOut(tween(150)) + shrinkVertically(tween(150)))
                        },
                        label = "repeatMode"
                    ) { mode ->
                        when (mode) {
                            RepeatMode.ONCE -> Text(
                                "Klingelt nur einmal: " + (previewNext?.let { triggerDayLabel(it) }
                                    ?: "bald") + " – danach wird der Wecker automatisch deaktiviert.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            RepeatMode.DATE -> Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showDatePicker = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(12.dp))
                                Column {
                                    Text(
                                        "Weckdatum",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        dateEpochDay?.let { fullDateLabel(it) } ?: "Datum wählen",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            RepeatMode.WEEKLY -> Column {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    (0..6).forEach { day ->
                                        val selected = daysMask and (1 shl day) != 0
                                        FilterChip(
                                            selected = selected,
                                            onClick = { daysMask = daysMask xor (1 shl day) },
                                            label = { Text(dayLabel(day)) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (daysMask == 0) "Kein Tag gewählt → klingelt einmalig"
                                    else "Klingelt: ${daysSummary(Alarm(0, 0, 0, daysMask))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Bezeichnung (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Klingelton
                SectionCard(contentAlignment = Alignment.Start) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Klingelton wählen")
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                        ringtoneUri?.let(Uri::parse)
                                    )
                                }
                                ringtoneLauncher.launch(intent)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                "Klingelton",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(ringtoneTitle, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // Aufgabe zum Beenden
                SectionCard(title = "Aufgabe zum Beenden", contentAlignment = Alignment.Start) {
                    val challengeLabels = listOf("Keine", "Mathe", "Barcode", "Schritte")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ChallengeType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = challengeType == type,
                                onClick = { challengeType = type },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = ChallengeType.entries.size
                                )
                            ) { Text(challengeLabels[index]) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AnimatedVisibility(
                        visible = challengeType != ChallengeType.NONE,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when (challengeType) {
                                ChallengeType.MATH -> {
                                    Text("Schwierigkeit:", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Leicht", "Mittel", "Schwer", "Sehr schwer")
                                            .forEachIndexed { level, name ->
                                                FilterChip(
                                                    selected = mathDifficulty == level,
                                                    onClick = { mathDifficulty = level },
                                                    label = { Text(name) }
                                                )
                                            }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Anzahl Aufgaben: $mathCount",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = mathCount.toFloat(),
                                        onValueChange = { mathCount = it.roundToInt() },
                                        valueRange = 1f..5f,
                                        steps = 3
                                    )
                                }
                                ChallengeType.BARCODE -> {
                                    Button(onClick = onLearnBarcode) {
                                        Text(
                                            if (barcodeValue == null) "Barcode einlernen"
                                            else "Barcode neu einlernen"
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (barcodeValue != null) "Barcode gespeichert ✓"
                                        else "Scanne den Barcode, den du morgens suchen musst (z. B. auf der Duschgel-Flasche).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (barcodeValue != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                                }
                                ChallengeType.STEPS -> {
                                    Text(
                                        "Schritte: $stepGoal",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = stepGoal.toFloat(),
                                        onValueChange = { stepGoal = (it / 10).roundToInt() * 10 },
                                        valueRange = 10f..200f,
                                        steps = 18
                                    )
                                }
                                ChallengeType.NONE -> Unit
                            }
                        }
                    }
                }

                // Aufwachkontrolle
                SectionCard(title = "Aufwachkontrolle", contentAlignment = Alignment.Start) {
                    Text(
                        if (wakeCheckMinutes == 0) "Aus"
                        else "Klingelt $wakeCheckMinutes Min nach dem Beenden erneut, bis du in der Benachrichtigung endgültig deaktivierst",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (wakeCheckMinutes == 0) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = wakeCheckMinutes.toFloat(),
                        onValueChange = { wakeCheckMinutes = (it / 5).roundToInt() * 5 },
                        valueRange = 0f..30f,
                        steps = 5
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Aus", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                        Text("30 Min", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateEpochDay
                ?.let { LocalDate.ofEpochDay(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                ?: LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        dateEpochDay = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Karte für einen Einstellungs-Bereich im Edit-Screen. */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = contentAlignment
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}
