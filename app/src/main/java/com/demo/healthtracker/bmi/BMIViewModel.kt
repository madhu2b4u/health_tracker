package com.demo.healthtracker.bmi

import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.units.Mass
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.BmiData
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
class BMIViewModel @Inject constructor(private val healthManager: HealthManager) : ViewModel() {

    private val _bmiData = MutableStateFlow<List<BmiData>>(emptyList())
    val bmiData: StateFlow<List<BmiData>> = _bmiData.asStateFlow()

    private val _selectedHeight = MutableStateFlow(1.7f)
    val selectedHeight = _selectedHeight.asStateFlow()

    private val _selectedWeight = MutableStateFlow(70.0f)
    val selectedWeight = _selectedWeight.asStateFlow()

    private val _bodyFatPercentage = MutableStateFlow<Float?>(null)
    val bodyFatPercentage = _bodyFatPercentage.asStateFlow()

    init {
        loadBmiData()
    }

    fun loadBmiData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS) // Get data for the last 30 days
            _bmiData.value = healthManager.readBmiData(startTime, endTime)
        }
    }

    fun updateHeight(height: Float) {
        _selectedHeight.value = height
    }

    fun updateWeight(weight: Float) {
        _selectedWeight.value = weight
    }

    fun updateBodyFatPercentage(percentage: Float?) {
        _bodyFatPercentage.value = percentage
    }

    fun calculateBmi(): Float {
        val height = _selectedHeight.value
        val weight = _selectedWeight.value
        return weight / (height * height)
    }

    fun getBmiCategory(): String {
        val bmi = calculateBmi()
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal"
            bmi < 25.9 -> "Slightly Overweight"
            bmi < 30 -> "Overweight"
            bmi < 35 -> "Obesity Class I"
            bmi < 40 -> "Obesity Class II"
            else -> "Obesity Class III"
        }
    }

    fun calculateLeanBodyMass(gender: String = "male"): Float {
        val height = _selectedHeight.value * 100 // convert to cm
        val weight = _selectedWeight.value

        // If body fat percentage is provided, use it to calculate lean body mass
        _bodyFatPercentage.value?.let {
            return weight * (1 - it / 100f)
        }

        // Otherwise use Boer formula
        return if (gender == "male") {
            0.407f * weight + 0.267f * height - 19.2f
        } else {
            0.252f * weight + 0.473f * height - 48.3f
        }
    }

    fun saveBmiData(gender: String = "male") {
        viewModelScope.launch {
            healthManager.writeBmiData(
                _selectedHeight.value,
                _selectedWeight.value,
                _bodyFatPercentage.value
            )
            loadBmiData() // Refresh data after saving
        }
    }
}