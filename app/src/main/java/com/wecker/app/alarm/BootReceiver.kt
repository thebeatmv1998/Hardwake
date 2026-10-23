package com.wecker.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wecker.app.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Nach einem Neustart werden alle aktiven Wecker neu geplant. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmRepository(context).rescheduleAllEnabled()
            } finally {
                pending.finish()
            }
        }
    }
}
