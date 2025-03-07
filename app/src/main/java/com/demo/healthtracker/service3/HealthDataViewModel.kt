package com.demo.healthtracker.service3

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthDataMonitorV3
import com.demo.healthtracker.HealthMetric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class HealthDataViewModel @Inject constructor(
    private val healthDataMonitor: HealthDataMonitorV3
) : ViewModel() {

    private val _healthData = MutableStateFlow<List<HealthMetric>>(emptyList())
    val healthData: StateFlow<List<HealthMetric>> = _healthData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _availableMetricTypes = MutableStateFlow<List<String>>(emptyList())
    val availableMetricTypes: StateFlow<List<String>> = _availableMetricTypes.asStateFlow()

    // Daily summaries by date and metric type
    private val _dailySummaries = MutableStateFlow<Map<String, Map<String, Double>>>(emptyMap())
    val dailySummaries: StateFlow<Map<String, Map<String, Double>>> = _dailySummaries.asStateFlow()

    fun loadLast30DaysData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val endTime = Instant.now()
                val startTime = endTime.minus(30, ChronoUnit.DAYS)
                
                // Refresh data to ensure we have the latest
                healthDataMonitor.refreshHealthData(startTime, endTime)
                
                // Fetch the filtered data
                val metricsResult = healthDataMonitor.getHealthMetricsResultByTimeRange(startTime, endTime)
                _healthData.value = metricsResult.metrics
                
                // Extract available metric types (excluding diastolic BP as it's always shown with systolic)
                val allTypes = metricsResult.metricTypes.toMutableList()
                allTypes.remove("bloodpressure_diastolic") // We'll handle this special case with systolic
                _availableMetricTypes.value = allTypes.sorted()
                
                // Calculate daily summaries for steps and distance
                calculateDailySummaries(metricsResult.metrics)
                
                Log.d("HealthViewModel", "Loaded ${metricsResult.metrics.size} health metrics")
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Error loading health data", e)
                _healthData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshData() {
        loadLast30DaysData()
    }
    
    private fun calculateDailySummaries(metrics: List<HealthMetric>) {
        viewModelScope.launch {
            try {
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                
                // Group by date
                val groupedByDate = metrics.groupBy { metric ->
                    metric.intervalStartDate.split("T")[0]
                }
                
                // For each date, calculate summaries for specific metrics
                val summaryMap = mutableMapOf<String, MutableMap<String, Double>>()
                
                // Process each date
                groupedByDate.forEach { (date, dateMetrics) ->
                    val dateSummaries = mutableMapOf<String, Double>()
                    
                    // Calculate step count totals for each day
                    val stepMetrics = dateMetrics.filter { it.elementName == "stepcount" }
                    if (stepMetrics.isNotEmpty()) {
                        // Sum all step counts for the day
                        val totalSteps = stepMetrics.sumOf { metric ->
                            metric.value.toDoubleOrNull() ?: 0.0
                        }
                        dateSummaries["stepcount"] = totalSteps
                    }
                    
                    // Calculate distance totals for each day
                    val distanceMetrics = dateMetrics.filter { it.elementName == "distance" }
                    if (distanceMetrics.isNotEmpty()) {
                        // Sum all distances for the day
                        val totalDistance = distanceMetrics.sumOf { metric ->
                            metric.value.toDoubleOrNull() ?: 0.0
                        }
                        dateSummaries["distance"] = totalDistance
                    }
                    
                    // Calculate average heart rate
                    val heartRateMetrics = dateMetrics.filter { it.elementName == "heartrate" }
                    if (heartRateMetrics.isNotEmpty()) {
                        val values = heartRateMetrics.mapNotNull { it.value.toDoubleOrNull() }
                        if (values.isNotEmpty()) {
                            dateSummaries["heartrate"] = values.average()
                        }
                    }
                    
                    // Calculate average blood oxygen
                    val oxygenMetrics = dateMetrics.filter { it.elementName == "bloodoxygen" }
                    if (oxygenMetrics.isNotEmpty()) {
                        val values = oxygenMetrics.mapNotNull { it.value.toDoubleOrNull() }
                        if (values.isNotEmpty()) {
                            dateSummaries["bloodoxygen"] = values.average()
                        }
                    }
                    
                    // Calculate sleep duration
                    val sleepMetrics = dateMetrics.filter { it.elementName == "sleep" }
                    if (sleepMetrics.isNotEmpty()) {
                        val totalSleep = sleepMetrics.sumOf { metric ->
                            metric.value.toDoubleOrNull() ?: 0.0
                        }
                        dateSummaries["sleep"] = totalSleep
                    }
                    
                    // Store summaries for this date
                    if (dateSummaries.isNotEmpty()) {
                        summaryMap[date] = dateSummaries
                    }
                }
                
                _dailySummaries.value = summaryMap
                Log.d("HealthViewModel", "Calculated daily summaries for ${summaryMap.size} days")
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Error calculating daily summaries", e)
            }
        }
    }
    
    /**
     * Get aggregated metrics for a specific date
     */
    fun getMetricsForDate(date: String): List<HealthMetric> {
        return _healthData.value.filter {
            it.intervalStartDate.startsWith(date)
        }
    }
    
    /**
     * Get the most recent value for a specific metric type
     */
    fun getMostRecentMetric(metricType: String): HealthMetric? {
        return _healthData.value
            .filter { it.elementName == metricType }
            .maxByOrNull { it.intervalEndDate }
    }
    
    /**
     * Get metrics by type
     */
    fun getMetricsByType(metricType: String): List<HealthMetric> {
        return _healthData.value.filter { it.elementName == metricType }
    }
    
    /**
     * Generate date labels for the last 30 days (for chart usage)
     */
    fun getLast30DaysLabels(): List<String> {
        val formatter = DateTimeFormatter.ofPattern("MMM d")
        val today = LocalDate.now()
        
        return (0 until 30).map { daysAgo ->
            today.minusDays(daysAgo.toLong()).format(formatter)
        }.reversed()
    }
    
    /**
     * Get daily data series for a specific metric (for chart usage)
     */
    fun getDailyDataSeries(metricType: String): List<Double> {
        val today = LocalDate.now()
        val results = mutableListOf<Double>()
        
        // For each of the last 30 days
        for (i in 29 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            // Get the summary value for this date and metric type
            val value = _dailySummaries.value[dateString]?.get(metricType) ?: 0.0
            results.add(value)
        }
        
        return results
    }
}