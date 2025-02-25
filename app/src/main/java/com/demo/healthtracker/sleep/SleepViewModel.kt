package com.demo.healthtracker.sleep

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class SleepViewModel @Inject constructor(private val healthManager: HealthManager) :
    ViewModel() {

    private val _sleepData = MutableStateFlow<List<SleepSessionRecord>>(emptyList())
    val sleepData: StateFlow<List<SleepSessionRecord>> = _sleepData.asStateFlow()

    init {
        loadSleepData()
    }

    private fun loadSleepData() {
        viewModelScope.launch {
            val systemZone = ZoneId.systemDefault()
            val endTime = LocalDateTime.now().atZone(systemZone).toInstant()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _sleepData.value = healthManager.readSleepData(startTime, endTime)
                .sortedByDescending { it.startTime }
        }
    }

    fun addSleepSession(startTime: LocalDateTime, endTime: LocalDateTime) {
        viewModelScope.launch {
            val currentTime = LocalDateTime.now()
            if (startTime.isBefore(currentTime) && endTime.isBefore(currentTime)) {
                // Convert using system default zone instead of UTC
                val systemZone = ZoneId.systemDefault()
                healthManager.writeSleepData(
                    startTime = startTime.atZone(systemZone).toInstant(),
                    endTime = endTime.atZone(systemZone).toInstant(),
                    stage = SleepSessionRecord.STAGE_TYPE_SLEEPING
                )
                loadSleepData()
            }
        }
    }
}