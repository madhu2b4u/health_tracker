package com.demo.healthtracker.service

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.demo.healthtracker.HealthManager
import com.demo.healthtracker.formatDateTime
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
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataMonitor @Inject constructor(
    private val healthManager: HealthManager,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null

    private val _healthData = MutableStateFlow<HealthData?>(null)
    val healthData: StateFlow<HealthData?> = _healthData.asStateFlow()

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }


    private val healthPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    private suspend fun checkPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(healthPermissions)
        } catch (e: Exception) {
            Log.e("HealthMonitor", "Error checking permissions", e)
            false
        }
    }

    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                try {

                    if (!checkPermissions()) {
                        Log.d("HealthMonitor", "Permissions not granted, waiting...")
                        delay(35000)
                        continue
                    }

                    val endTime = Instant.now()
                    val startTime = endTime.minus(7, ChronoUnit.DAYS)

                   /* Log.d(
                        "HealthMonitor",
                        "Starting health data check at ${formatDateTime(endTime)}"
                    )*/

                    // Get data and explicitly sort by time before taking latest
                    val latestSteps = healthManager.readStepsData(startTime, endTime)
                        .sortedByDescending { it.startTime }
                        .firstOrNull()

                    val latestHeartRate = healthManager.readHeartRateData(startTime, endTime)
                        .sortedByDescending { it.startTime }
                        .firstOrNull()

                    val latestBloodPressure =
                        healthManager.readBloodPressureData(startTime, endTime)
                            .sortedByDescending { it.time }
                            .firstOrNull()

                    val latestBloodOxygen = healthManager.readBloodOxygenData(startTime, endTime)
                        .sortedByDescending { it.time }
                        .firstOrNull()

                    val latestRespiratory = healthManager.readRespiratoryData(startTime, endTime)
                        .sortedByDescending { it.time }
                        .firstOrNull()

                    val latestWorkout = healthManager.readWorkoutData(startTime, endTime)
                        .sortedByDescending { it.startTime }
                        .firstOrNull()

                    val latestSleep = healthManager.readSleepData(startTime, endTime)
                        .sortedByDescending { it.startTime }
                        .firstOrNull()

                    // Log with timestamps to verify ordering
                  /*  Log.d(
                        "HealthMonitor", """
                    Latest Health Data:
                    Steps: ${latestSteps?.count ?: "No data"} at ${latestSteps?.startTime}
                    Heart Rate: ${latestHeartRate?.samples?.firstOrNull()?.beatsPerMinute ?: "No data"} at ${latestHeartRate?.startTime}
                    Blood Pressure: ${latestBloodPressure?.systolic?.inMillimetersOfMercury?.toInt()}/${latestBloodPressure?.diastolic?.inMillimetersOfMercury?.toInt() ?: "No data"} at ${latestBloodPressure?.time}
                    Blood Oxygen: ${latestBloodOxygen?.percentage?.value ?: "No data"}% at ${latestBloodOxygen?.time}
                    Respiratory Rate: ${latestRespiratory?.rate ?: "No data"} at ${latestRespiratory?.time}
                    Latest Workout: ${latestWorkout?.title ?: "No data"} at ${latestWorkout?.startTime}
                    Latest Sleep: From ${latestSleep?.startTime} to ${latestSleep?.endTime}
                    """.trimIndent()
                    )*/

                    val data = HealthData(
                        steps = listOfNotNull(latestSteps),
                        heartRate = listOfNotNull(latestHeartRate),
                        bloodPressure = listOfNotNull(latestBloodPressure),
                        bloodOxygen = listOfNotNull(latestBloodOxygen),
                        respiratory = listOfNotNull(latestRespiratory),
                        workout = listOfNotNull(latestWorkout),
                        sleep = listOfNotNull(latestSleep)
                    )

                    _healthData.value = data

                    //Log.d("HealthMonitor", "Health data check completed")
                    //Log.d("HealthMonitor", "-------------------------------------------")
                    delay(33000)
                } catch (e: Exception) {
                    Log.e("HealthMonitor", "Error monitoring health data", e)
                    delay(35000)
                }
            }
        }
       // Log.i("HealthMonitor", "Health monitoring started")
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Log.i("HealthMonitor", "Health monitoring stopped")
    }
}