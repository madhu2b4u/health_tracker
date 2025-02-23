package com.demo.healthtracker.steps

import android.content.Context
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class StepsViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val healthManager = HealthManager(context)

    private val _stepsData = MutableStateFlow<List<StepsRecord>>(emptyList())
    val stepsData: StateFlow<List<StepsRecord>> = _stepsData.asStateFlow()

    init {
        loadStepsData()
    }

    private fun loadStepsData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(14, ChronoUnit.DAYS) // Last 7 days
            _stepsData.value = healthManager.readStepsData(startTime, endTime)
                .sortedByDescending { it.startTime }
        }
    }

    fun addSteps(count: Long) {
        viewModelScope.launch {
            healthManager.writeStepsData(count)
            loadStepsData()
        }
    }
}