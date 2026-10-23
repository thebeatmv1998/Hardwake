package com.wecker.app.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wecker.app.challenge.BarcodeScanner
import com.wecker.app.i18n.LocalStrings

/** Beim Anlegen eines Weckers: Barcode einscannen und speichern. */
@Composable
fun BarcodeLearnScreen(onScanned: (String) -> Unit) {
    val s = LocalStrings.current
    var scanned by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            s.learnBarcodeTitle,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            s.learnBarcodeHelp,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        BarcodeScanner(
            modifier = Modifier.fillMaxWidth().weight(1f),
            onBarcode = { value ->
                if (!scanned) {
                    scanned = true
                    onScanned(value)
                }
            }
        )
    }
}
