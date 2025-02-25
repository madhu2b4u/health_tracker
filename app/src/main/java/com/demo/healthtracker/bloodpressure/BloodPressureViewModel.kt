package com.demo.healthtracker.bloodpressure

import androidx.health.connect.client.records.BloodPressureRecord
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
class BloodPressureViewModel @Inject constructor(private val healthManager: HealthManager) :
    ViewModel() {

    private val _bloodPressureData = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    val bloodPressureData: StateFlow<List<BloodPressureRecord>> = _bloodPressureData.asStateFlow()

    init {
        loadBloodPressureData()
    }

    fun loadBloodPressureData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _bloodPressureData.value = healthManager.readBloodPressureData(startTime, endTime)
        }
    }

    fun addBloodPressure(systolic: Double, diastolic: Double) {
        viewModelScope.launch {
            healthManager.writeBloodPressureData(systolic, diastolic)
            loadBloodPressureData()
        }
    }

    fun getBloodPressureCategory(systolic: Double, diastolic: Double): String {
        return when {
            systolic < 120 && diastolic < 80 -> "Normal"
            systolic < 130 && diastolic < 80 -> "Elevated"
            systolic < 140 || diastolic < 90 -> "Stage 1"
            else -> "Stage 2"
        }
    }
}