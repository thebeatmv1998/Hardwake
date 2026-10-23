package com.wecker.app

import android.app.Application
import com.wecker.app.alarm.NotificationHelper
import com.wecker.app.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeckerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        // Beim App-Start alle Wecker neu planen und abgelaufene Einmal-Wecker
        // automatisch deaktivieren.
        CoroutineScope(Dispatchers.Default).launch {
            AlarmRepository(this@WeckerApp).rescheduleAllEnabled()
        }
    }
}
