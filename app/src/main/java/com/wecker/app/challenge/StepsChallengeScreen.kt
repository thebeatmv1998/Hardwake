package com.wecker.app.challenge

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** Schritte-Challenge: [goal] Schritte laufen, um den Wecker zu beenden. */
@Composable
fun StepsChallengeScreen(goal: Int, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var steps by remember { mutableIntStateOf(0) }
    var sensorMissing by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    LaunchedEffect(Unit) {
        if (!hasPermission && Build.VERSION.SDK_INT >= 29) {
            launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    DisposableEffect(hasPermission) {
        if (!hasPermission) return@DisposableEffect onDispose {}
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            sensorMissing = true
            return@DisposableEffect onDispose {}
        }
        // TYPE_STEP_COUNTER zählt seit dem Booten → Baseline beim Start merken.
        var baseline: Float? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values[0]
                val base = baseline ?: total.also { baseline = it }
                val walked = (total - base).toInt()
                steps = walked
                if (!done && walked >= goal) {
                    done = true
                    onSuccess()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Geh $goal Schritte", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))
        when {
            sensorMissing -> Text(
                "Dieses Gerät hat keinen Schrittzähler-Sensor.",
                color = MaterialTheme.colorScheme.error
            )
            !hasPermission -> Text("Berechtigung für körperliche Aktivität erforderlich")
            else -> {
                CircularProgressIndicator(
                    progress = { (steps / goal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 10.dp
                )
                Spacer(Modifier.height(24.dp))
                Text("$steps / $goal Schritte", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
