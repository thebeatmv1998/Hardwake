package com.wecker.app

import android.app.Application
import com.wecker.app.alarm.NotificationHelper
import com.wecker.app.data.AlarmRepository
import com.wecker.app.data.SettingsRepository
import com.wecker.app.i18n.AppLanguage
import com.wecker.app.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeckerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Kanalnamen in der Systemsprache anlegen; der Collector unten aktualisiert
        // sie, sobald die gespeicherte Sprache geladen ist (oder sich ändert).
        NotificationHelper.createChannels(this, stringsFor(AppLanguage.SYSTEM))
        CoroutineScope(Dispatchers.Default).launch {
            SettingsRepository(this@WeckerApp).language.collect { language ->
                NotificationHelper.createChannels(this@WeckerApp, stringsFor(language))
            }
        }
        // Beim App-Start alle Wecker neu planen und abgelaufene Einmal-Wecker
        // automatisch deaktivieren.
        CoroutineScope(Dispatchers.Default).launch {
            AlarmRepository(this@WeckerApp).rescheduleAllEnabled()
        }
    }
}
