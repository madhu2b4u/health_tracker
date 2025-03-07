package com.demo.healthtracker.service2

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.units.Length
import com.demo.healthtracker.HealthManager
import com.demo.healthtracker.formatDateTime
import com.demo.healthtracker.formatDuration
import com.demo.healthtracker.service.HealthDataV2
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class HealthDataMonitorV2 @Inject constructor(
    private val healthManager: HealthManager,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private var lastRefreshTime = 0L

    private val _healthData = MutableStateFlow<HealthDataV2?>(null)
    val healthData: StateFlow<HealthDataV2?> = _healthData.asStateFlow()

    private var changesToken: String? = null

    @RequiresApi(Build.VERSION_CODES.S)
    fun startMonitoring() {
        Log.i("HealthMonitor", "Starting health data monitoring...")
        stopMonitoring()

        // Perform initial data load
        Log.d("HealthMonitor", "Performing initial data load")
        refreshData()

        // Start monitoring for changes
        monitoringJob = scope.launch {
            try {
                Log.d("HealthMonitor", "Setting up Health Connect changes monitoring")

                // Get initial token
                val tokenRequest = ChangesTokenRequest(
                    recordTypes = setOf(
                        StepsRecord::class,
                        HeartRateRecord::class,
                        BloodPressureRecord::class,
                        OxygenSaturationRecord::class,
                        RespiratoryRateRecord::class,
                        ExerciseSessionRecord::class,
                        SleepSessionRecord::class,
                        LeanBodyMassRecord::class,
                        DistanceRecord::class
                    )
                )
                changesToken = healthConnectClient.getChangesToken(tokenRequest)
                Log.d("HealthMonitor", "Initial changes token: $changesToken")

                // Start periodic checking for changes
                while (isActive) {
                    delay(30000) // Check every 30 seconds

                    val currentToken = changesToken
                    if (currentToken != null) {
                        try {
                            val changesResponse = healthConnectClient.getChanges(currentToken)
                            if (changesResponse.changesTokenExpired) {
                                Log.d("HealthMonitor", "Changes token expired, getting new token")
                                changesToken = healthConnectClient.getChangesToken(tokenRequest)
                            } else if (changesResponse.hasMore) {
                                Log.d("HealthMonitor", "Health data changes detected")
                                refreshData()
                                // Update token after detecting changes
                                changesToken = healthConnectClient.getChangesToken(tokenRequest)
                            } else {
                                Log.d("HealthMonitor", "No health data changes detected")
                            }
                        } catch (e: Exception) {
                            Log.e("HealthMonitor", "Error checking for changes, getting new token", e)
                            changesToken = healthConnectClient.getChangesToken(tokenRequest)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HealthMonitor", "Error setting up change monitoring", e)
                e.printStackTrace()
            }
        }
        Log.i("HealthMonitor", "Health monitoring started successfully")
    }

    // Public method to refresh data with throttling
    @RequiresApi(Build.VERSION_CODES.S)
    fun refreshData() {
        val currentTime = System.currentTimeMillis()
        // Only refresh if at least 3 seconds have passed since last refresh
        if (currentTime - lastRefreshTime > 3000) {
            lastRefreshTime = currentTime
            refreshHealthData()
        } else {
            Log.d("HealthMonitor", "Refresh request throttled")
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun refreshHealthData() {
        scope.launch {
            try {
                Log.d("HealthMonitor", "Starting health data refresh...")
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)
                Log.d("HealthMonitor", "Fetching data from ${formatDateTime(startTime)} to ${formatDateTime(endTime)}")

                // Instead of just getting the latest steps record, get daily totals
                val dailyStepsMap = mutableMapOf<LocalDate, Long>()
                val currentDate = LocalDate.now()
                val startDate = currentDate.minusDays(7)

                // Calculate daily totals for each day in the range
                var iterDate = startDate
                while (!iterDate.isAfter(currentDate)) {
                    val dayStart = iterDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val dayEnd = iterDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                    // Get all steps records for this day and sum them
                    val daySteps = healthManager.readStepsData(dayStart, dayEnd)
                        .sumOf { it.count }

                    if (daySteps > 0) {
                        dailyStepsMap[iterDate] = daySteps
                    }

                    iterDate = iterDate.plusDays(1)
                }

                // Log the daily totals
                dailyStepsMap.forEach { (date, steps) ->
                    Log.d("HealthMonitor", "Steps for ${date}: $steps steps")
                }

                val latestDayWithSteps = dailyStepsMap.entries
                    .maxByOrNull { it.key }
                    ?.let { (date, count) ->
                        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                        StepsRecord(
                            count = count,
                            startTime = dayStart,
                            endTime = dayEnd,
                            startZoneOffset = ZoneId.systemDefault().rules.getOffset(dayStart),
                            endZoneOffset = ZoneId.systemDefault().rules.getOffset(dayEnd)
                        )
                    }

                Log.d("HealthMonitor", "Latest day's steps: ${latestDayWithSteps?.count ?: "No data"}")

                val latestHeartRate =
                    healthManager.readHeartRateData(startTime, endTime).maxByOrNull { it.startTime }
                Log.d("HealthMonitor", "Heart rate data: ${latestHeartRate?.samples?.firstOrNull()?.beatsPerMinute ?: "No data"}")

                val latestBloodPressure =
                    healthManager.readBloodPressureData(startTime, endTime).maxByOrNull { it.time }
                Log.d("HealthMonitor", "Blood pressure data: ${latestBloodPressure?.systolic?.inMillimetersOfMercury?.toInt() ?: "No data"}/${latestBloodPressure?.diastolic?.inMillimetersOfMercury?.toInt() ?: ""}")

                val latestBloodOxygen =
                    healthManager.readBloodOxygenData(startTime, endTime).maxByOrNull { it.time }
                Log.d("HealthMonitor", "Blood oxygen data: ${latestBloodOxygen?.percentage?.value ?: "No data"}%")

                val latestRespiratory =
                    healthManager.readRespiratoryData(startTime, endTime).maxByOrNull { it.time }
                Log.d("HealthMonitor", "Respiratory data: ${latestRespiratory?.rate ?: "No data"} breaths/min")

                val latestWorkout =
                    healthManager.readWorkoutData(startTime, endTime).maxByOrNull { it.startTime }
                Log.d("HealthMonitor", "Workout data: ${latestWorkout?.title ?: "No data"}")

                val latestSleep =
                    healthManager.readSleepData(startTime, endTime).maxByOrNull { it.startTime }
                Log.d("HealthMonitor", "Sleep data: Duration ${if (latestSleep != null) formatDuration(latestSleep.startTime, latestSleep.endTime) else "No data"}")

                val latestBmi = healthManager.readRawBmiData(startTime, endTime).maxByOrNull { it.time }
                Log.d("HealthMonitor", "BMI data: ${latestBmi?.mass?.inKilograms ?: "No data"} kg")

                val dailyDistanceMap = mutableMapOf<LocalDate, Double>()
                // Calculate daily totals for distance
                iterDate = startDate
                while (!iterDate.isAfter(currentDate)) {
                    val dayStart = iterDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val dayEnd = iterDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                    // Get all distance records for this day and sum them
                    val dayDistance = healthManager.readDistanceData(dayStart, dayEnd)
                        .sumOf { it.distance.inKilometers }

                    if (dayDistance > 0) {
                        dailyDistanceMap[iterDate] = dayDistance
                    }

                    iterDate = iterDate.plusDays(1)
                }

                dailyDistanceMap.forEach { (date, distance) ->
                    Log.d("HealthMonitor", "Distance for ${date}: $distance km")
                }

                val latestDayWithDistance = dailyDistanceMap.entries
                    .maxByOrNull { it.key }
                    ?.let { (date, totalDistance) ->
                        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                        DistanceRecord(
                            distance = Length.kilometers(totalDistance),
                            startTime = dayStart,
                            endTime = dayEnd,
                            startZoneOffset = ZoneId.systemDefault().rules.getOffset(dayStart),
                            endZoneOffset = ZoneId.systemDefault().rules.getOffset(dayEnd),
                        )
                    }


                val newData = HealthDataV2(
                    steps = listOfNotNull(latestDayWithSteps),
                    heartRate = listOfNotNull(latestHeartRate),
                    bloodPressure = listOfNotNull(latestBloodPressure),
                    bloodOxygen = listOfNotNull(latestBloodOxygen),
                    respiratory = listOfNotNull(latestRespiratory),
                    workout = listOfNotNull(latestWorkout),
                    sleep = listOfNotNull(latestSleep),
                    bmi = listOfNotNull(latestBmi),
                    distance = listOfNotNull(latestDayWithDistance)
                )

                // Only update if data has actually changed
                val currentData = _healthData.value
                if (currentData != newData) {
                    _healthData.value = newData
                    Log.d("HealthMonitor", "Health data updated due to changes")
                } else {
                    Log.d("HealthMonitor", "No actual data changes found")
                }

                Log.d("HealthMonitor", "Health data refresh completed successfully")
                Log.d("HealthMonitor", "-------------------------------------------")
            } catch (e: Exception) {
                Log.e("HealthMonitor", "Error refreshing health data", e)
                e.printStackTrace()
            }
        }
    }

    fun stopMonitoring() {
        if (monitoringJob != null) {
            Log.i("HealthMonitor", "Stopping health monitoring...")
            monitoringJob?.cancel()
            monitoringJob = null
            Log.i("HealthMonitor", "Health monitoring stopped successfully")
        }
    }


}