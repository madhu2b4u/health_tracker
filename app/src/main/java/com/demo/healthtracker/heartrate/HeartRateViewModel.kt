package com.demo.healthtracker.heartrate

import android.content.Context
import androidx.health.connect.client.records.HeartRateRecord
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
class HeartRateViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val healthManager = HealthManager(context)

    private val _heartRateData = MutableStateFlow<List<HeartRateRecord>>(emptyList())
    val heartRateData: StateFlow<List<HeartRateRecord>> = _heartRateData.asStateFlow()

    init {
        loadHeartRateData()
    }

    private fun loadHeartRateData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _heartRateData.value = healthManager.readHeartRateData(startTime, endTime)
                .sortedByDescending { it.startTime }
        }
    }

    fun addHeartRate(bpm: Long) {
        viewModelScope.launch {
            healthManager.writeHeartRateData(bpm)
            loadHeartRateData()
        }
    }
}