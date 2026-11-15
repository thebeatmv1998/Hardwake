package com.wecker.app

import android.app.Application
import com.wecker.app.alarm.NotificationHelper
import com.wecker.app.data.AlarmRepository
import com.wecker.app.data.SettingsRepository
import com.wecker.app.i18n.AppLanguage
import com.wecker.app.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        // Beim App-Start alle Wecker neu planen, abgelaufene Einmal-Wecker
        // deaktivieren und aktive Aufwachkontrollen wiederherstellen.
        CoroutineScope(Dispatchers.Default).launch {
            val settings = SettingsRepository(this@WeckerApp)
            val strings = stringsFor(settings.language.first())
            val repository = AlarmRepository(this@WeckerApp)
            repository.rescheduleAllEnabled()
            repository.restoreWakeChecks(strings)
        }
    }
}
