package com.wecker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wecker.app.alarm.AlarmScheduler
import com.wecker.app.data.Alarm
import com.wecker.app.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AlarmRepository(app)

    /** Sortiert nach dem nächsten Auslösezeitpunkt; abgelaufene landen hinten. */
    val alarms: StateFlow<List<Alarm>> = repository.alarms
        .map { list -> list.sortedBy { AlarmScheduler.nextTriggerMillis(it) ?: Long.MAX_VALUE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { repository.save(alarm.copy(enabled = enabled)) }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch { repository.delete(alarm) }
    }

    /** Nächstes Vorkommen einmalig ausfallen lassen. */
    fun skipNext(alarm: Alarm) {
        viewModelScope.launch { repository.skipNext(alarm.id) }
    }

    /** Aussetzen rückgängig machen. */
    fun clearSkip(alarm: Alarm) {
        viewModelScope.launch { repository.clearSkip(alarm.id) }
    }
}
