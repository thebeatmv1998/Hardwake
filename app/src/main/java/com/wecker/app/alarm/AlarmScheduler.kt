package com.wecker.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wecker.app.data.Alarm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Plant exakte Alarme über den AlarmManager und berechnet den nächsten Auslösezeitpunkt. */
object AlarmScheduler {

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    /**
     * Nächster Zeitpunkt (Millis), an dem der Wecker klingeln soll – oder null,
     * wenn kein künftiger Termin mehr existiert (z. B. Datum in der Vergangenheit).
     * Ein gesetztes [Alarm.skipEpochDay] lässt genau dieses Vorkommen ausfallen.
     */
    fun nextTriggerMillis(alarm: Alarm, from: LocalDateTime = LocalDateTime.now()): Long? {
        val first = nextTriggerAfter(alarm, from) ?: return null
        val skipDay = alarm.skipEpochDay ?: return first
        val firstDate = Instant.ofEpochMilli(first).atZone(ZoneId.systemDefault()).toLocalDate()
        if (firstDate.toEpochDay() != skipDay) return first
        // Einmalige und Datums-Wecker haben kein weiteres Vorkommen.
        if (alarm.dateEpochDay != null || alarm.daysMask == 0) return null
        return nextTriggerAfter(alarm, firstDate.plusDays(1).atStartOfDay())
    }

    /** Nächstes Vorkommen ohne Berücksichtigung von [Alarm.skipEpochDay]. */
    private fun nextTriggerAfter(alarm: Alarm, from: LocalDateTime): Long? {
        // Konkretes Datum: genau einmal an diesem Tag klingeln, sonst nie.
        alarm.dateEpochDay?.let { epochDay ->
            val date = LocalDate.ofEpochDay(epochDay)
            if (date.isBefore(from.toLocalDate())) return null
            val candidate = date.atTime(alarm.hour, alarm.minute)
            if (!candidate.isAfter(from)) return null
            return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        if (alarm.daysMask == 0) {
            // Einmalig: heute, falls die Zeit noch kommt, sonst morgen.
            var candidate = from.withHour(alarm.hour).withMinute(alarm.minute)
                .withSecond(0).withNano(0)
            if (!candidate.isAfter(from)) candidate = candidate.plusDays(1)
            return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        // daysMask: Bit 0 = Montag … Bit 6 = Sonntag; LocalDateTime.dayOfWeek.value: 1 = Montag.
        for (offset in 0..7) {
            val day = from.plusDays(offset.toLong())
            val bit = day.dayOfWeek.value - 1
            if (alarm.daysMask and (1 shl bit) == 0) continue
            val candidate = day.withHour(alarm.hour).withMinute(alarm.minute)
                .withSecond(0).withNano(0)
            if (candidate.isAfter(from)) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
        // Sollte nie erreicht werden (Bitmask nicht leer), Fallback: morgen.
        return from.plusDays(1).withHour(alarm.hour).withMinute(alarm.minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) return
        val triggerAt = nextTriggerMillis(alarm) ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                alarmPendingIntent(context, alarm.id)
            )
        } catch (e: SecurityException) {
            // Exakte Alarme nicht erlaubt → ungenauer Fallback, Hinweis zeigt die Liste.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, alarmPendingIntent(context, alarm.id))
        }
    }

    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(context, alarmId))
    }

    /**
     * Plant die Aufwachkontrolle bzw. den Snooze: erneutes Klingeln in [minutes] Minuten.
     * Liefert den Auslösezeitpunkt (Millis), damit die Benachrichtigung denselben
     * Zeitpunkt für den Countdown verwenden kann.
     */
    fun scheduleWakeCheck(context: Context, alarmId: Long, minutes: Int): Long {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        try {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                wakeCheckPendingIntent(context, alarmId)
            )
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, wakeCheckPendingIntent(context, alarmId))
        }
        return triggerAt
    }

    fun cancelWakeCheck(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(wakeCheckPendingIntent(context, alarmId))
    }

    private fun alarmPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun wakeCheckPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, WakeCheckReceiver::class.java).apply {
            action = WakeCheckReceiver.ACTION_WAKE_CHECK
            putExtra(WakeCheckReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context, alarmId.toInt() + 1_000_000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
