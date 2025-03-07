package com.demo.healthtracker.workmanager

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.demo.healthtracker.HealthManager
import com.demo.healthtracker.formatDateTime
import com.demo.healthtracker.formatDuration
import com.demo.healthtracker.service.HealthDataV2
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class HealthDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthManager: HealthManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "health_data_monitoring_work"
        private const val TAG = "HealthDataWorker"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "Starting health data refresh from worker...")
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            Log.d(TAG, "Fetching data from ${formatDateTime(startTime)} to ${formatDateTime(endTime)}")

            val latestSteps = 
                healthManager.readStepsData(startTime, endTime).maxByOrNull { it.startTime }
            Log.d(TAG, "Steps data: ${latestSteps?.count ?: "No data"}")

            val latestHeartRate =
                healthManager.readHeartRateData(startTime, endTime).maxByOrNull { it.startTime }
            Log.d(TAG, "Heart rate data: ${latestHeartRate?.samples?.firstOrNull()?.beatsPerMinute ?: "No data"}")

            val latestBloodPressure =
                healthManager.readBloodPressureData(startTime, endTime).maxByOrNull { it.time }
            Log.d(TAG, "Blood pressure data: ${latestBloodPressure?.systolic?.inMillimetersOfMercury?.toInt() ?: "No data"}/${latestBloodPressure?.diastolic?.inMillimetersOfMercury?.toInt() ?: ""}")

            val latestBloodOxygen =
                healthManager.readBloodOxygenData(startTime, endTime).maxByOrNull { it.time }
            Log.d(TAG, "Blood oxygen data: ${latestBloodOxygen?.percentage?.value ?: "No data"}%")

            val latestRespiratory =
                healthManager.readRespiratoryData(startTime, endTime).maxByOrNull { it.time }
            Log.d(TAG, "Respiratory data: ${latestRespiratory?.rate ?: "No data"} breaths/min")

            val latestWorkout =
                healthManager.readWorkoutData(startTime, endTime).maxByOrNull { it.startTime }
            Log.d(TAG, "Workout data: ${latestWorkout?.title ?: "No data"}")

            val latestSleep =
                healthManager.readSleepData(startTime, endTime).maxByOrNull { it.startTime }
            Log.d(TAG, "Sleep data: Duration ${if (latestSleep != null) formatDuration(latestSleep.startTime, latestSleep.endTime) else "No data"}")

            val latestBmi =
                healthManager.readRawBmiData(startTime, endTime).maxByOrNull { it.time }
            Log.d(TAG, "BMI data: ${latestBmi?.mass?.inKilograms ?: "No data"} kg")

            val latestDistance =
                healthManager.readDistanceData(startTime, endTime).maxByOrNull { it.startTime }
            Log.d(TAG, "Distance data: ${latestDistance?.distance?.inKilometers ?: "No data"} km")

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

            // Update the data in the repository/manager
            HealthDataRepository.updateHealthData(newData)
            
            Log.d(TAG, "Health data refresh completed successfully from worker")
            Log.d(TAG, "-------------------------------------------")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing health data", e)
            e.printStackTrace()
            return Result.failure()
        }
    }
}
