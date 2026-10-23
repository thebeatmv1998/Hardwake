package com.wecker.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.wecker.app.MainActivity
import com.wecker.app.data.Alarm
import com.wecker.app.i18n.Strings
import com.wecker.app.ui.RingActivity

object NotificationHelper {
    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_WAKE_CHECK = "wake_check"

    fun ringNotificationId(alarmId: Long) = 10_000 + alarmId.toInt()
    fun wakeCheckNotificationId(alarmId: Long) = 20_000 + alarmId.toInt()

    fun createChannels(context: Context, strings: Strings) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, strings.channelAlarms, NotificationManager.IMPORTANCE_HIGH).apply {
                description = strings.channelAlarmsDesc
                setBypassDnd(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WAKE_CHECK, strings.channelWakeGuard, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = strings.channelWakeGuardDesc
            }
        )
    }

    /** Full-Screen-Notification, die die RingActivity über dem Sperrbildschirm öffnet. */
    fun buildRingNotification(context: Context, alarm: Alarm, strings: Strings): Notification {
        val fullScreen = PendingIntent.getActivity(
            context, alarm.id.toInt(),
            Intent(context, RingActivity::class.java).apply {
                putExtra(RingActivity.EXTRA_ALARM_ID, alarm.id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val time = "%02d:%02d".format(alarm.hour, alarm.minute)
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(strings.ringTitle(time))
            .setContentText(alarm.label.ifBlank { strings.ringFallbackText })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Antippen der (schwebenden) Benachrichtigung öffnet die Aufgabe direkt.
            // Ohne ContentIntent reagiert die Heads-up-Benachrichtigung auf keinen Tipp.
            .setContentIntent(fullScreen)
            .addAction(0, strings.solveAction, fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .build()
    }

    /**
     * Aufwachkontrolle/Snooze: Nutzer muss den Wecker hier endgültig deaktivieren.
     * Die Benachrichtigung zeigt die verbleibende Zeit bis zum nächsten Klingeln
     * als live mitlaufenden Countdown (Chronometer).
     *
     * @param triggerAtMillis Zeitpunkt des nächsten Klingelns (= AlarmManager-Termin).
     */
    fun showWakeCheckNotification(
        context: Context,
        alarm: Alarm,
        triggerAtMillis: Long,
        strings: Strings
    ) {
        val dismissIntent = PendingIntent.getBroadcast(
            context, alarm.id.toInt() + 500_000,
            Intent(context, WakeCheckReceiver::class.java).apply {
                action = WakeCheckReceiver.ACTION_FINAL_DISMISS
                putExtra(WakeCheckReceiver.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openAppIntent = PendingIntent.getActivity(
            context, alarm.id.toInt() + 600_000,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_WAKE_CHECK)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(strings.wakeGuardTitle)
            .setContentText(strings.wakeGuardText)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent)
            // Restzeit bis zum erneuten Klingeln live herunterzählen.
            .setWhen(triggerAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .addAction(0, strings.finalDismiss, dismissIntent)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(wakeCheckNotificationId(alarm.id), notification)
    }

    fun cancelWakeCheckNotification(context: Context, alarmId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(wakeCheckNotificationId(alarmId))
    }

    fun cancelRingNotification(context: Context, alarmId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ringNotificationId(alarmId))
    }
}
