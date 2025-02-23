package com.demo.healthtracker.bmi

import android.content.Context
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.units.Mass
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
class BMIViewModel @Inject constructor(@ApplicationContext private val context: Context) :
    ViewModel() {

    private val healthManager = HealthManager(context)

    private val _bmiData = MutableStateFlow<List<LeanBodyMassRecord>>(emptyList())
    val bmiData: StateFlow<List<LeanBodyMassRecord>> = _bmiData.asStateFlow()

    init {
        loadBmiData()
    }

    private fun loadBmiData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _bmiData.value = healthManager.readBmiData(startTime, endTime)
        }
    }

    fun addBmi(height: Float, weight: Float) {
        viewModelScope.launch {
            healthManager.writeBmiData(height, weight)
            loadBmiData()
        }
    }

    fun getBmiCategory(mass: Mass): String {
        val bmi = mass.inKilograms
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }
}