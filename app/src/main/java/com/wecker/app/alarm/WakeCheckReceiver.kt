package com.wecker.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.wecker.app.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Aufwachkontrolle: Klingelt der Wecker erneut (ACTION_WAKE_CHECK) oder wird
 * er über die Benachrichtigung endgültig deaktiviert (ACTION_FINAL_DISMISS)?
 */
class WakeCheckReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WAKE_CHECK = "com.wecker.app.WAKE_CHECK"
        const val ACTION_FINAL_DISMISS = "com.wecker.app.FINAL_DISMISS"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        NotificationHelper.cancelWakeCheckNotification(context, alarmId)

        when (intent.action) {
            ACTION_WAKE_CHECK -> {
                // Nutzer hat nicht deaktiviert → Wecker klingelt erneut.
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            ACTION_FINAL_DISMISS -> {
                // Wichtig: den geplanten Kontroll-Alarm canceln, sonst klingelt er trotzdem.
                AlarmScheduler.cancelWakeCheck(context, alarmId)
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmRepository(context).rescheduleNext(alarmId)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
