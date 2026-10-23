package com.wecker.app.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Problem(val text: String, val answer: Int)

private fun generateProblem(difficulty: Int, index: Int): Problem {
    // Innerhalb einer Stufe steigt die Schwierigkeit leicht mit der Aufgabennummer.
    val bonus = index * 5
    return when (difficulty) {
        // Leicht: einfache Addition/Subtraktion im kleinen Zahlenraum.
        0 -> {
            val a = Random.nextInt(2, 20 + bonus)
            val b = Random.nextInt(2, 15 + bonus)
            if (Random.nextBoolean() || a < b) Problem("$a + $b", a + b)
            else Problem("$a − $b", a - b)
        }
        // Schwer: dreistellige Addition/Subtraktion, größere Multiplikationen, Division.
        2 -> when (Random.nextInt(4)) {
            0 -> {
                val a = Random.nextInt(120, 500 + bonus * 10)
                val b = Random.nextInt(80, a)
                Problem("$a − $b", a - b)
            }
            1 -> {
                val a = Random.nextInt(100, 400 + bonus * 10)
                val b = Random.nextInt(100, 400 + bonus * 10)
                Problem("$a + $b", a + b)
            }
            2 -> {
                val a = Random.nextInt(6, 20)
                val b = Random.nextInt(6, 20)
                Problem("$a × $b", a * b)
            }
            else -> {
                val b = Random.nextInt(3, 13)
                val result = Random.nextInt(5, 16)
                Problem("${b * result} ÷ $b", result)
            }
        }
        // Sehr schwer: dreigliedrige Terme, große Multiplikationen, große Divisionen.
        3 -> when (Random.nextInt(4)) {
            0 -> {
                val a = Random.nextInt(50, 300)
                val b = Random.nextInt(50, 300)
                val c = Random.nextInt(20, minOf(a + b - 1, 250))
                Problem("$a + $b − $c", a + b - c)
            }
            1 -> {
                val a = Random.nextInt(12, 26)
                val b = Random.nextInt(12, 26)
                Problem("$a × $b", a * b)
            }
            2 -> {
                val b = Random.nextInt(7, 20)
                val result = Random.nextInt(11, 25)
                Problem("${b * result} ÷ $b", result)
            }
            else -> {
                val a = Random.nextInt(7, 16)
                val b = Random.nextInt(7, 16)
                val c = Random.nextInt(30, 99)
                Problem("$a × $b + $c", a * b + c)
            }
        }
        // Mittel (Standard): zweistellige Addition/Subtraktion, kleine Multiplikationen.
        else -> when (Random.nextInt(3)) {
            0 -> {
                val a = Random.nextInt(10, 60 + bonus)
                val b = Random.nextInt(10, 40 + bonus)
                Problem("$a + $b", a + b)
            }
            1 -> {
                val a = Random.nextInt(20, 90 + bonus)
                val b = Random.nextInt(10, a)
                Problem("$a − $b", a - b)
            }
            else -> {
                val a = Random.nextInt(3, 13)
                val b = Random.nextInt(3, 13)
                Problem("$a × $b", a * b)
            }
        }
    }
}

/** Rechenaufgabe: [count] Aufgaben in Schwierigkeit [difficulty] (0 = Leicht … 3 = Sehr schwer) richtig lösen. */
@Composable
fun MathChallengeScreen(count: Int, difficulty: Int, onSuccess: () -> Unit) {
    var solved by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(generateProblem(difficulty, 0)) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit(fromKeyboard: Boolean = false) {
        // Verhindert eine Doppel-Auswertung, wenn IME-Action und Enter-Taste
        // beide ausgelöst werden: nach dem Absenden ist das Feld bereits leer.
        if (fromKeyboard && input.isBlank()) return
        if (input.trim().toIntOrNull() == problem.answer) {
            solved++
            input = ""
            if (solved >= count) onSuccess() else problem = generateProblem(difficulty, solved)
        } else {
            error = true
        }
    }

    // Scrollbar + imePadding: „Prüfen" bleibt auch mit offener Tastatur erreichbar.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Rechenaufgabe", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { solved / count.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text("Aufgabe ${solved + 1} von $count", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))
        Text("${problem.text} = ?", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = false },
            label = { Text("Antwort") },
            isError = error,
            supportingText = { if (error) Text("Falsch – versuch es nochmal") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit(fromKeyboard = true) }),
            singleLine = true,
            // Hardware-Tastatur: Enter/Return löst die Aufgabe ebenfalls aus.
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    submit(fromKeyboard = true)
                    true
                } else {
                    false
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { submit() }) {
            Text("Prüfen")
        }
    }
}
