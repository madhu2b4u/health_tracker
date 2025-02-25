package com.demo.healthtracker.distance

import androidx.health.connect.client.records.DistanceRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DistanceViewModel @Inject constructor(private val healthManager: HealthManager) :
    ViewModel() {

    private val _distanceData = MutableStateFlow<List<DistanceRecord>>(emptyList())
    val distanceData: StateFlow<List<DistanceRecord>> = _distanceData.asStateFlow()

    init {
        loadDistanceData()
    }

    private fun loadDistanceData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS)
            _distanceData.value = healthManager.readDistanceData(startTime, endTime)
        }
    }

    fun addDistance(distance: Double) {
        viewModelScope.launch {
            healthManager.writeDistanceData(distance)
            loadDistanceData()
        }
    }

    fun getTotalDistance(): Double {
        return _distanceData.value.sumOf { it.distance.inKilometers }
    }
}