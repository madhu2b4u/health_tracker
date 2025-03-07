package com.demo.healthtracker.workmanager

import android.util.Log
import com.demo.healthtracker.service.HealthDataV2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HealthDataRepository {
    private val _healthData = MutableStateFlow<HealthDataV2?>(null)
    val healthData: StateFlow<HealthDataV2?> = _healthData.asStateFlow()

    fun updateHealthData(data: HealthDataV2) {
        val currentData = _healthData.value
        if (currentData != data) {
            _healthData.value = data
            Log.d("HealthDataRepo", "Health data updated with new values")
        } else {
            Log.d("HealthDataRepo", "No actual data changes found")
        }
    }
}