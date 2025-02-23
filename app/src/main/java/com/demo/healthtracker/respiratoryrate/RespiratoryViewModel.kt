package com.demo.healthtracker.respiratoryrate

import android.content.Context
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
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
class RespiratoryViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    
    private val healthManager = HealthManager(context)
    
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