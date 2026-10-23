package com.wecker.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wecker.app.data.Alarm
import com.wecker.app.data.AlarmRepository

class AlarmEditViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AlarmRepository(app)

    suspend fun load(alarmId: Long): Alarm? =
        if (alarmId > 0) repository.getById(alarmId) else null

    suspend fun save(alarm: Alarm): Long = repository.save(alarm)
}
