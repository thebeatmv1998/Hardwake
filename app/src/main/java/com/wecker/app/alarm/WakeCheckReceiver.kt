package com.wecker.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wecker.app.data.AlarmRepository
import com.wecker.app.data.SettingsRepository
import com.wecker.app.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Aufwachkontrolle: Klingelt der Wecker erneut (ACTION_WAKE_CHECK), wird er
 * endgültig deaktiviert (ACTION_FINAL_DISMISS) oder wurde die Benachrichtigung
 * weggewischt (ACTION_NOTIFICATION_DISMISSED)?
 */
class WakeCheckReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WAKE_CHECK = "com.wecker.app.WAKE_CHECK"
        const val ACTION_FINAL_DISMISS = "com.wecker.app.FINAL_DISMISS"
        const val ACTION_NOTIFICATION_DISMISSED = "com.wecker.app.WAKE_CHECK_DISMISSED"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return

        if (intent.action == ACTION_NOTIFICATION_DISMISSED) {
            // Android 14+ erlaubt das Wegwischen dauerhafter Benachrichtigungen.
            // Solange die Kontrolle läuft, die Benachrichtigung wieder aufbauen.
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    restoreNotification(context, alarmId)
                } finally {
                    pending.finish()
                }
            }
            return
        }

        NotificationHelper.cancelWakeCheckNotification(context, alarmId)

        when (intent.action) {
            ACTION_WAKE_CHECK -> {
                // Nutzer hat nicht deaktiviert → Wecker klingelt erneut.
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                // Die Kontrolle geht in das Klingeln über; nur eine fällige
                // Kontrolle löschen, keine bereits neu geplante.
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmRepository(context).clearWakeCheckIfDue(alarmId)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_FINAL_DISMISS -> {
                // Wichtig: den geplanten Kontroll-Alarm canceln, sonst klingelt er trotzdem.
                AlarmScheduler.cancelWakeCheck(context, alarmId)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmRepository(context).cancelWakeCheck(alarmId)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    /** Zeigt die Aufwachkontrolle erneut an, falls sie noch aktiv ist. */
    private suspend fun restoreNotification(context: Context, alarmId: Long) {
        val alarm = AlarmRepository(context).getById(alarmId) ?: return
        val triggerAt = alarm.wakeCheckUntil ?: return
        if (triggerAt <= System.currentTimeMillis()) return
        val strings = stringsFor(SettingsRepository(context).language.first())
        NotificationHelper.showWakeCheckNotification(context, alarm, triggerAt, strings)
    }
}
