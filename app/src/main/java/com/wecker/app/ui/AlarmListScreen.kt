package com.wecker.app.ui

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wecker.app.alarm.AlarmScheduler
import com.wecker.app.data.Alarm
import com.wecker.app.data.ChallengeType
import com.wecker.app.data.SettingsRepository
import com.wecker.app.data.ThemeMode
import com.wecker.app.ui.theme.AppPalettes
import com.wecker.app.ui.theme.LocalAppPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlarmListScreen(
    onEditAlarm: (Long) -> Unit,
    viewModel: AlarmListViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val colorScheme by settings.colorScheme.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Alarm?>(null) }

    // Hinweise bei jedem Resume neu bewerten, damit sie direkt nach dem
    // Erteilen einer Berechtigung verschwinden.
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentColor = colorScheme,
            currentMode = themeMode,
            onSelectColor = { scope.launch { settings.setColorScheme(it) } },
            onSelectMode = { scope.launch { settings.setThemeMode(it) } },
            onDismiss = { showThemeDialog = false }
        )
    }

    pendingDelete?.let { victim ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Wecker löschen?") },
            text = {
                Text(
                    "Der Wecker %02d:%02d (%s) wird endgültig entfernt."
                        .format(victim.hour, victim.minute, repeatSummary(victim))
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(victim)
                    pendingDelete = null
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = { ScaleInFab(onClick = { onEditAlarm(0L) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientHeader(
                title = "Wecker",
                tagline = "Zuverlässig aufwachen",
                actions = {
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Design-Einstellungen",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            )
            NextAlarmCountdown(alarms, onEditAlarm)
            // resumeTick lesen, damit die Bedingungen bei jedem Resume neu geprüft werden.
            @Suppress("UNUSED_EXPRESSION") resumeTick
            HintCard(
                visible = batteryOptimizationNeeded(context),
                text = "Akku-Optimierung ist aktiv – Samsung legt die App sonst schlafen und der Wecker kann ausfallen.",
                actionLabel = "Ausnahme",
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
            HintCard(
                visible = fullScreenIntentBlocked(context),
                text = "Vollbild-Alarme sind deaktiviert – der Wecker öffnet sich dann nicht automatisch auf dem Sperrbildschirm.",
                actionLabel = "Erlauben",
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
            HintCard(
                visible = exactAlarmsBlocked(context),
                text = "Exakte Alarme sind deaktiviert – der Wecker könnte verspätet klingeln.",
                actionLabel = "Erlauben",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            )
            if (alarms.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 96.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            modifier = Modifier.animateItem(),
                            onClick = { onEditAlarm(alarm.id) },
                            onToggle = { viewModel.setEnabled(alarm, it) },
                            onDelete = { pendingDelete = alarm }
                        )
                    }
                }
            }
        }
    }
}

/** FAB, der beim Erschließen sanft hochskaliert. */
@Composable
private fun ScaleInFab(onClick: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fabScale"
    )
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Wecker") }
    )
}

/** Getönte Ergebnis-Card mit Countdown bis zum nächsten Klingeln (Zeitrechner-Stil). */
@Composable
private fun NextAlarmCountdown(alarms: List<Alarm>, onEditAlarm: (Long) -> Unit) {
    val palette = LocalAppPalette.current
    // Ticker: einmal pro Sekunde neu berechnen.
    val now by produceState(System.currentTimeMillis(), alarms) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val next = alarms.filter { it.enabled }
        .mapNotNull { alarm -> AlarmScheduler.nextTriggerMillis(alarm)?.let { alarm to it } }
        .minByOrNull { it.second } ?: return

    val (alarm, triggerAt) = next
    val totalSec = ((triggerAt - now) / 1000).coerceAtLeast(0)
    val days = totalSec / 86400
    val hours = (totalSec % 86400) / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val ticking = if (days > 0) "%d T  %02d:%02d:%02d".format(days, hours, minutes, seconds)
    else "%02d:%02d:%02d".format(hours, minutes, seconds)

    // Pulsierender Live-Punkt.
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    Card(
        onClick = { onEditAlarm(alarm.id) },
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.result)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha))
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Nächster Wecker · %02d:%02d · %s".format(
                        alarm.hour, alarm.minute, triggerDayLabel(triggerAt)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = ticking,
                transitionSpec = {
                    (slideInVertically(tween(250)) { it / 3 } + fadeIn(tween(250)))
                        .togetherWith(slideOutVertically(tween(250)) { -it / 3 } + fadeOut(tween(250)))
                },
                label = "countdown"
            ) { text ->
                Text(
                    text,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                relativeLabel(totalSec),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Leerer Zustand mit sanftem Einblend-Effekt. */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "emptyScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(500),
        label = "emptyAlpha"
    )
    Column(
        modifier = modifier.padding(24.dp).scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Noch keine Wecker", style = MaterialTheme.typography.titleMedium, modifier = Modifier.alpha(alpha))
        Text(
            "Tippe auf „Wecker“, um deinen ersten zu erstellen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.alpha(alpha)
        )
    }
}

/** Hinweis-Karte, die sich beim Auftauchen/Verschwinden animiert ein-/ausklappt. */
@Composable
private fun HintCard(visible: Boolean, text: String, actionLabel: String, onAction: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

private fun batteryOptimizationNeeded(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return !pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun fullScreenIntentBlocked(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return false
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return !nm.canUseFullScreenIntent()
}

private fun exactAlarmsBlocked(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 31) return false
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return !am.canScheduleExactAlarms()
}

/** Dialog zur Design-Auswahl: Farbschema + Modus (wie Zeitrechner). */
@Composable
private fun ThemeDialog(
    currentColor: Int,
    currentMode: ThemeMode,
    onSelectColor: (Int) -> Unit,
    onSelectMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Design") },
        text = {
            Column {
                Text("Farbe", style = MaterialTheme.typography.labelLarge)
                AppPalettes.forEachIndexed { index, (light, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectColor(index) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentColor == index, onClick = { onSelectColor(index) })
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(light.headerStart, light.headerEnd)))
                        )
                        Text(light.themeName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Modus", style = MaterialTheme.typography.labelLarge)
                listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Hell",
                    ThemeMode.DARK to "Dunkel"
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentMode == mode, onClick = { onSelectMode(mode) })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

@Composable
private fun AlarmRow(
    alarm: Alarm,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val next = remember(alarm.id, alarm.enabled, alarm.hour, alarm.minute, alarm.daysMask, alarm.dateEpochDay) {
        AlarmScheduler.nextTriggerMillis(alarm)
    }
    val expired = alarm.dateEpochDay != null && next == null
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "%02d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline
                )
                val details = buildList {
                    add(repeatSummary(alarm))
                    if (alarm.label.isNotBlank()) add(alarm.label)
                    add(
                        when (alarm.challengeType) {
                            ChallengeType.NONE -> "Keine Aufgabe"
                            ChallengeType.MATH -> "Rechenaufgabe (${listOf("Leicht", "Mittel", "Schwer", "Sehr schwer")[alarm.mathDifficulty.coerceIn(0, 3)]})"
                            ChallengeType.BARCODE -> "Barcode"
                            ChallengeType.STEPS -> "${alarm.stepGoal} Schritte"
                        }
                    )
                    if (alarm.wakeCheckMinutes > 0) add("Aufwachkontrolle ${alarm.wakeCheckMinutes} Min")
                }
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                when {
                    alarm.enabled && next != null ->
                        Text(
                            "Klingelt " + relativeLabel((next - System.currentTimeMillis()).coerceAtLeast(0) / 1000),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    expired ->
                        Text(
                            "Abgelaufen",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    !alarm.enabled ->
                        Text(
                            "Deaktiviert",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                }
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen")
            }
        }
    }
}
