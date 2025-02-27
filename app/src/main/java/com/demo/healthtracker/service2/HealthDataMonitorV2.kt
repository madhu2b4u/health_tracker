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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
@RequiresApi(Build.VERSION_CODES.S)
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

    private fun refreshHealthData() {
        scope.launch {
            try {
                Log.d("HealthMonitor", "Starting health data refresh...")
                val endTime = Instant.now()
                val startTime = endTime.minus(7, ChronoUnit.DAYS)
                Log.d("HealthMonitor", "Fetching data from ${formatDateTime(startTime)} to ${formatDateTime(endTime)}")

                val latestSteps =
                    healthManager.readStepsData(startTime, endTime).maxByOrNull { it.startTime }
                Log.d("HealthMonitor", "Steps data: ${latestSteps?.count ?: "No data"}")

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

                val latestBmi =
                    healthManager.readBmiData(startTime, endTime).maxByOrNull { it.time }
                Log.d("HealthMonitor", "BMI data: ${latestBmi?.mass?.inKilograms ?: "No data"} kg")

                val latestDistance =
                    healthManager.readDistanceData(startTime, endTime).maxByOrNull { it.startTime }
                Log.d("HealthMonitor", "Distance data: ${latestDistance?.distance?.inKilometers ?: "No data"} km")

                val newData = HealthDataV2(
                    steps = listOfNotNull(latestSteps),
                    heartRate = listOfNotNull(latestHeartRate),
                    bloodPressure = listOfNotNull(latestBloodPressure),
                    bloodOxygen = listOfNotNull(latestBloodOxygen),
                    respiratory = listOfNotNull(latestRespiratory),
                    workout = listOfNotNull(latestWorkout),
                    sleep = listOfNotNull(latestSleep),
                    bmi = listOfNotNull(latestBmi),
                    distance = listOfNotNull(latestDistance)
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