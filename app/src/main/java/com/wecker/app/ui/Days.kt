package com.wecker.app.ui

import com.wecker.app.data.Alarm
import com.wecker.app.i18n.Strings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun dayLabel(index: Int, s: Strings) = s.dayShort[index]

/** Kurztext der Klingeltage, z. B. „Mo, Mi, Fr", „Täglich" oder „Once". */
fun daysSummary(alarm: Alarm, s: Strings): String = when {
    alarm.daysMask == 0 -> s.oneTime
    alarm.daysMask == 0b1111111 -> s.everyDay
    alarm.daysMask == 0b0011111 -> s.weekdaysRange
    else -> s.dayShort.filterIndexed { i, _ -> alarm.daysMask and (1 shl i) != 0 }
        .joinToString(s.listSeparator)
}

/** Wiederholung eines Weckers: konkretes Datum oder Wochentage. */
fun repeatSummary(alarm: Alarm, s: Strings): String = when (val epochDay = alarm.dateEpochDay) {
    null -> daysSummary(alarm, s)
    else -> dateLabel(epochDay, s)
}

/** Kompaktes Datum, z. B. „Mo, 26.10.2026" oder „Mon, Oct 26, 2026". */
fun dateLabel(epochDay: Long, s: Strings): String =
    LocalDate.ofEpochDay(epochDay).format(dateFormatter(s))

/** Datum eines Auslösezeitpunkts: „heute", „morgen" oder das konkrete Datum. */
fun triggerDayLabel(millis: Long, s: Strings, now: LocalDate = LocalDate.now()): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (date) {
        now -> s.today
        now.plusDays(1) -> s.tomorrow
        else -> date.format(dateFormatter(s))
    }
}

/** Ausführliches Datum für den Datum-Picker. */
fun fullDateLabel(epochDay: Long, s: Strings): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(s.fullDatePattern, s.locale))

/** Relativer Abstand in Worten, z. B. „in 3 Tagen" oder „in 3 days". */
fun relativeLabel(totalSeconds: Long, s: Strings): String = s.relativeTime(totalSeconds)

private fun dateFormatter(s: Strings): DateTimeFormatter =
    DateTimeFormatter.ofPattern(s.datePattern, s.locale)
