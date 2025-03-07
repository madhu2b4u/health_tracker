package com.demo.healthtracker.steps

import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class StepsViewModel @Inject constructor(private val healthManager: HealthManager) :
    ViewModel() {

    private val _stepsData = MutableStateFlow<List<StepsRecord>>(emptyList())
    val stepsData: StateFlow<List<StepsRecord>> = _stepsData.asStateFlow()

    private val _dailyStepsData = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    val dailyStepsData: StateFlow<Map<LocalDate, Long>> = _dailyStepsData.asStateFlow()


    init {
        loadDailyStepsData()
    }

    private fun loadStepsData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(14, ChronoUnit.DAYS) // Last 7 days
            _stepsData.value = healthManager.readStepsData(startTime, endTime)
                .sortedByDescending { it.startTime }
        }
    }

    private fun loadDailyStepsData() {
        viewModelScope.launch {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(14) // Last 14 days

            // Create a map to store results
            val dailyTotals = mutableMapOf<LocalDate, Long>()

            // Get total steps for each day in the range
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val steps = healthManager.readTotalStepsForDay(currentDate)
                dailyTotals[currentDate] = steps
                currentDate = currentDate.plusDays(1)
            }

            _dailyStepsData.value = dailyTotals.toSortedMap(compareByDescending { it })
        }
    }

    fun addSteps(count: Long) {
        viewModelScope.launch {
            healthManager.writeStepsData(count)
            loadStepsData()
        }
    }
}