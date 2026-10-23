package com.wecker.app.data

import android.content.Context
import com.wecker.app.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val context: Context) {
    private val dao = AlarmDatabase.get(context).alarmDao()

    val alarms: Flow<List<Alarm>> = dao.observeAll()

    suspend fun getById(id: Long): Alarm? = dao.getById(id)

    suspend fun save(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) dao.insert(alarm) else {
            dao.update(alarm)
            alarm.id
        }
        var saved = alarm.copy(id = id)
        // Smart: Einmal-Wecker mit bereits verstrichenem Termin werden nicht
        // scharf geschaltet, sondern automatisch deaktiviert.
        if (saved.enabled && AlarmScheduler.nextTriggerMillis(saved) == null) {
            saved = saved.copy(enabled = false)
            dao.update(saved)
        }
        if (saved.enabled) AlarmScheduler.schedule(context, saved)
        else AlarmScheduler.cancel(context, id)
        return id
    }

    suspend fun delete(alarm: Alarm) {
        AlarmScheduler.cancel(context, alarm.id)
        AlarmScheduler.cancelWakeCheck(context, alarm.id)
        dao.delete(alarm)
    }

    /** Nach dem endgültigen Deaktivieren: nächstes Vorkommen planen (oder Einmal-Wecker abschalten). */
    suspend fun rescheduleNext(alarmId: Long) {
        val alarm = dao.getById(alarmId) ?: return
        if (alarm.daysMask == 0) {
            dao.update(alarm.copy(enabled = false))
        } else {
            AlarmScheduler.schedule(context, alarm)
        }
    }

    /** Plant alle aktiven Wecker neu; abgelaufene Einmal-/Datums-Wecker werden deaktiviert. */
    suspend fun rescheduleAllEnabled() {
        dao.getEnabled().forEach { alarm ->
            if (AlarmScheduler.nextTriggerMillis(alarm) == null) {
                dao.update(alarm.copy(enabled = false))
            } else {
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }
}
