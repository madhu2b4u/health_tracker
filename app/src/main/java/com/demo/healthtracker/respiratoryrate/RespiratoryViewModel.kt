package com.demo.healthtracker.respiratoryrate

import androidx.health.connect.client.records.RespiratoryRateRecord
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
class RespiratoryViewModel @Inject constructor(private val healthManager: HealthManager) :
    ViewModel() {

    private val _respiratoryData = MutableStateFlow<List<RespiratoryRateRecord>>(emptyList())
    val respiratoryData: StateFlow<List<RespiratoryRateRecord>> = _respiratoryData.asStateFlow()

    init {
        loadRespiratoryData()
    }

    private fun loadRespiratoryData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _respiratoryData.value = healthManager.readRespiratoryData(startTime, endTime)
        }
    }

    fun addRespiratoryRate(rate: Double) {
        viewModelScope.launch {
            healthManager.writeRespiratoryData(rate)
            loadRespiratoryData()
        }
    }

    fun getRateCategory(rate: Double): String {
        return when {
            rate < 12 -> "Below Normal"
            rate <= 20 -> "Normal"
            else -> "Above Normal"
        }
    }
}