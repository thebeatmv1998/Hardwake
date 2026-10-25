package com.wecker.app.data

import android.content.Context
import com.wecker.app.alarm.AlarmScheduler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    /**
     * „Nächsten überspringen": merkt sich den Tag des nächsten Vorkommens und
     * plant direkt den Termin danach. Nur für wiederkehrende Wecker sinnvoll.
     */
    suspend fun skipNext(alarmId: Long): Alarm? {
        val alarm = dao.getById(alarmId) ?: return null
        if (alarm.dateEpochDay != null || alarm.daysMask == 0) return null
        val next = AlarmScheduler.nextTriggerMillis(alarm) ?: return null
        val day = Instant.ofEpochMilli(next)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
        val updated = alarm.copy(skipEpochDay = day)
        dao.update(updated)
        AlarmScheduler.cancel(context, alarm.id)
        AlarmScheduler.schedule(context, updated)
        return updated
    }

    /** Hebt ein gesetztes Aussetzen wieder auf. */
    suspend fun clearSkip(alarmId: Long): Alarm? {
        val alarm = dao.getById(alarmId) ?: return null
        val updated = alarm.copy(skipEpochDay = null)
        dao.update(updated)
        AlarmScheduler.cancel(context, alarm.id)
        AlarmScheduler.schedule(context, updated)
        return updated
    }

    /** Nach dem endgültigen Deaktivieren: nächstes Vorkommen planen (oder Einmal-Wecker abschalten). */
    suspend fun rescheduleNext(alarmId: Long) {
        val alarm = dao.getById(alarmId) ?: return
        // Abgelaufene Aussetzer aufräumen, damit sie nicht dauerhaft in der DB stehen.
        val today = LocalDate.now().toEpochDay()
        val current = if (alarm.skipEpochDay != null && alarm.skipEpochDay < today) {
            alarm.copy(skipEpochDay = null).also { dao.update(it) }
        } else {
            alarm
        }
        if (current.daysMask == 0) {
            dao.update(current.copy(enabled = false))
        } else {
            AlarmScheduler.schedule(context, current)
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
