package com.wecker.app.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.wecker.app.alarm.AlarmService
import com.wecker.app.data.Alarm
import com.wecker.app.data.AlarmRepository
import com.wecker.app.data.ChallengeType
import com.wecker.app.data.SettingsRepository
import com.wecker.app.data.ThemeMode
import com.wecker.app.challenge.BarcodeChallengeScreen
import com.wecker.app.challenge.MathChallengeScreen
import com.wecker.app.challenge.StepsChallengeScreen
import com.wecker.app.ui.theme.WeckerTheme

/** Standard-Snooze-Dauer in Minuten. */
private const val SNOOZE_MINUTES = 10

/**
 * Vollbild-Screen über dem Sperrbildschirm, solange der Wecker klingelt.
 * Zeigt die konfigurierte Aufgabe; erst nach Erfolg wird der Service beendet.
 * Zusätzlich gibt es überall einen Snooze-Knopf.
 */
class RingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Über dem Sperrbildschirm anzeigen + Display einschalten, ohne zu entsperren.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguard.requestDismissKeyguard(this, null)

        // Echtes Vollbild: Status- und Navigationsleisten ausblenden.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // Display an lassen, solange die Aufgabe offen ist.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        setContent {
            val settings = remember { SettingsRepository(applicationContext) }
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val colorScheme by settings.colorScheme.collectAsState(initial = 0)
            WeckerTheme(themeMode = themeMode, colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alarm by produceState<Alarm?>(initialValue = null, alarmId) {
                        value = if (alarmId >= 0) AlarmRepository(applicationContext).getById(alarmId) else null
                    }
                    val current = alarm
                    if (current != null) {
                        RingContent(
                            alarm = current,
                            onDismiss = { dismissAlarm(current.id) },
                            onSnooze = { snoozeAlarm(current.id, SNOOZE_MINUTES) }
                        )
                    }
                }
            }
        }
    }

    private fun dismissAlarm(alarmId: Long) {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
        })
        finish()
    }

    private fun snoozeAlarm(alarmId: Long, minutes: Int) {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmService.EXTRA_SNOOZE_MINUTES, minutes)
        })
        finish()
    }
}

@Composable
private fun RingContent(alarm: Alarm, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Challenge bzw. Klingel-Ansicht füllt den oberen Bereich; Snooze bleibt unten erreichbar.
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (alarm.challengeType) {
                ChallengeType.MATH -> MathChallengeScreen(
                    count = alarm.mathCount,
                    difficulty = alarm.mathDifficulty,
                    onSuccess = onDismiss
                )
                ChallengeType.BARCODE -> BarcodeChallengeScreen(
                    expectedValue = alarm.barcodeValue ?: "",
                    onSuccess = onDismiss
                )
                ChallengeType.STEPS -> StepsChallengeScreen(goal = alarm.stepGoal, onSuccess = onDismiss)
                ChallengeType.NONE -> PlainRingScreen(alarm = alarm, onDismiss = onDismiss)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(onClick = onSnooze) {
                Icon(Icons.Default.Snooze, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("In $SNOOZE_MINUTES Min erneut erinnern")
            }
        }
    }
}

/** Klingel-Ansicht ohne Aufgabe: riesige Zeit, Label und pulsierende Kreise. */
@Composable
private fun PlainRingScreen(alarm: Alarm, onDismiss: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    // Zwei versetzt phasensierte Ripples.
    val transition = rememberInfiniteTransition(label = "ripple")
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "phase1"
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, delayMillis = 1300, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "phase2"
    )
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1300, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(340.dp)) {
            fun ripple(phase: Float) {
                val radius = (size.minDimension / 2f) * (0.55f + 0.45f * phase)
                drawCircle(
                    color = primary.copy(alpha = (1f - phase) * 0.35f),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = primary.copy(alpha = (1f - phase) * 0.08f),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            ripple(phase1)
            ripple(phase2)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Wecker",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "%02d:%02d".format(alarm.hour, alarm.minute),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.scale(breathe)
            )
            if (alarm.label.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    alarm.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(40.dp))
            Button(onClick = onDismiss) { Text("Wecker beenden") }
        }
    }
}
