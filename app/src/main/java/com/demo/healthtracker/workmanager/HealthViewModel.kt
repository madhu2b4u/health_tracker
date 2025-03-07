package com.demo.healthtracker.workmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.service.HealthDataV2
import com.demo.healthtracker.service2.HealthDataMonitorV2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthDataMonitorV2: HealthDataMonitorV2
) : ViewModel() {


    val healthData: StateFlow<HealthDataV2?> = healthDataMonitorV2.healthData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep alive for 5 seconds after last subscriber
            initialValue = null
        )

    init {
        startHealthMonitoring()
    }

    fun startHealthMonitoring() {
        healthDataMonitorV2.startMonitoring()
    }

    fun stopHealthMonitoring() {
        healthDataMonitorV2.stopMonitoring()
    }

    fun refreshHealthData() {
        healthDataMonitorV2.refreshData()
    }

    override fun onCleared() {
        super.onCleared()
        stopHealthMonitoring()
    }
}