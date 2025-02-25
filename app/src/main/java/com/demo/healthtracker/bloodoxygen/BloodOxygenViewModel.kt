package com.demo.healthtracker.bloodoxygen

import androidx.health.connect.client.records.OxygenSaturationRecord
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
class BloodOxygenViewModel @Inject constructor(private val healthManager: HealthManager) :
    ViewModel() {

    private val _bloodOxygenData = MutableStateFlow<List<OxygenSaturationRecord>>(emptyList())
    val bloodOxygenData: StateFlow<List<OxygenSaturationRecord>> = _bloodOxygenData.asStateFlow()

    init {
        loadBloodOxygenData()
    }

    fun loadBloodOxygenData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _bloodOxygenData.value = healthManager.readBloodOxygenData(startTime, endTime)
        }
    }

    fun addBloodOxygen(percentage: Double) {
        viewModelScope.launch {
            healthManager.writeBloodOxygenData(percentage)
            loadBloodOxygenData()
        }
    }

    fun getOxygenCategory(percentage: Double): String {
        return when {
            percentage >= 95 -> "Normal"
            percentage >= 90 -> "Mild Hypoxemia"
            percentage >= 85 -> "Moderate Hypoxemia"
            else -> "Severe Hypoxemia"
        }
    }
}