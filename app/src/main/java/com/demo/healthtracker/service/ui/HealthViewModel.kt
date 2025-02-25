package com.demo.healthtracker.service.ui

import androidx.lifecycle.ViewModel
import com.demo.healthtracker.service.HealthDataMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthDataMonitor: HealthDataMonitor
) : ViewModel() {

    val healthData = healthDataMonitor.healthData

    init {
        healthDataMonitor.startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        healthDataMonitor.stopMonitoring()
    }
}