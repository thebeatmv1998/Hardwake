package com.wecker.app.ui

import com.wecker.app.data.Alarm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_LABELS = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", Locale.GERMAN)

fun dayLabel(index: Int) = DAY_LABELS[index]

/** Kurztext der Klingeltage, z. B. „Mo, Mi, Fr", „Täglich" oder „Einmalig". */
fun daysSummary(alarm: Alarm): String = when {
    alarm.daysMask == 0 -> "Einmalig"
    alarm.daysMask == 0b1111111 -> "Täglich"
    alarm.daysMask == 0b0011111 -> "Mo–Fr"
    else -> DAY_LABELS.filterIndexed { i, _ -> alarm.daysMask and (1 shl i) != 0 }
        .joinToString(", ")
}

/** Wiederholung eines Weckers: konkretes Datum oder Wochentage, z. B. „Mi, 09.10.2028". */
fun repeatSummary(alarm: Alarm): String = when (val epochDay = alarm.dateEpochDay) {
    null -> daysSummary(alarm)
    else -> LocalDate.ofEpochDay(epochDay).format(DATE_FORMAT)
}

/** Datum eines Auslösezeitpunkts: „heute", „morgen" oder „Mo, 09.10.2028". */
fun triggerDayLabel(millis: Long, now: LocalDate = LocalDate.now()): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        now -> "heute"
        now.plusDays(1) -> "morgen"
        else -> date.format(DATE_FORMAT)
    }
}

/** Ausführliches Datum, z. B. „Montag, 09.10.2028" (für den Datum-Picker). */
fun fullDateLabel(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN))

/** Relativer Abstand in Worten, z. B. „in 3 Tagen", „in 2 Stunden", „in 2 Jahren und 40 Tagen". */
fun relativeLabel(totalSeconds: Long): String {
    val days = totalSeconds / 86_400
    return when {
        days >= 365 -> {
            val years = days / 365
            val rest = days % 365
            if (rest == 0L) "in $years " + (if (years == 1L) "Jahr" else "Jahren")
            else "in $years " + (if (years == 1L) "Jahr" else "Jahren") + " und $rest " +
                (if (rest == 1L) "Tag" else "Tagen")
        }
        days > 0 -> "in $days " + (if (days == 1L) "Tag" else "Tagen")
        else -> {
            val hours = totalSeconds / 3_600
            if (hours > 0) "in $hours " + (if (hours == 1L) "Stunde" else "Stunden")
            else {
                val minutes = totalSeconds / 60
                "in $minutes " + (if (minutes == 1L) "Minute" else "Minuten")
            }
        }
    }
}
