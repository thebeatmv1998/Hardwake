package com.wecker.app.challenge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wecker.app.i18n.LocalStrings

/** Barcode-Challenge: der eingelernte Barcode muss erneut gescannt werden. */
@Composable
fun BarcodeChallengeScreen(expectedValue: String, onSuccess: () -> Unit) {
    val s = LocalStrings.current
    var wrong by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            s.scanCorrectBarcode,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        BarcodeScanner(
            modifier = Modifier.fillMaxWidth().weight(1f),
            onBarcode = { value ->
                if (done) return@BarcodeScanner
                if (value == expectedValue) {
                    done = true
                    onSuccess()
                } else {
                    wrong = true
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (wrong) {
                Text(
                    s.wrongBarcode,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(s.holdBarcode)
            }
        }
    }
}
