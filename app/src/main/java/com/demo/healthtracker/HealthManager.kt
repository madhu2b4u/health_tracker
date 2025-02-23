package com.demo.healthtracker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


class HealthManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    // Heart Rate
    suspend fun readHeartRateData(startTime: Instant, endTime: Instant): List<HeartRateRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading heart rate data", e)
            emptyList()
        }
    }

    suspend fun writeHeartRateData(bpm: Long) {
        try {
            val currentTime = Instant.now()
            val heartRateRecord = HeartRateRecord(
                startTime = currentTime.minus(1, ChronoUnit.MINUTES), // Start time 1 minute ago
                startZoneOffset = ZoneOffset.UTC,
                endTime = currentTime, // Current time as end time
                endZoneOffset = ZoneOffset.UTC,
                samples = listOf(
                    HeartRateRecord.Sample(
                        time = currentTime.minus(
                            30,
                            ChronoUnit.SECONDS
                        ), // Sample time between start and end
                        beatsPerMinute = bpm
                    )
                )
            )
            healthConnectClient.insertRecords(listOf(heartRateRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing heart rate data", e)
        }
    }

    // Steps
    suspend fun readStepsData(startTime: Instant, endTime: Instant): List<StepsRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading steps data", e)
            emptyList()
        }
    }

    suspend fun writeStepsData(count: Long) {
        try {
            val stepsRecord = StepsRecord(
                startTime = Instant.now().minus(1, ChronoUnit.HOURS),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.now(),
                endZoneOffset = ZoneOffset.UTC,
                count = count
            )
            healthConnectClient.insertRecords(listOf(stepsRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing steps data", e)
        }
    }

    // Sleep
    suspend fun readSleepData(startTime: Instant, endTime: Instant): List<SleepSessionRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading sleep data", e)
            emptyList()
        }
    }

    suspend fun writeSleepData(startTime: Instant, endTime: Instant, stage: Int) {
        try {
            val systemZone = ZoneId.systemDefault()
            val zoneOffset = systemZone.rules.getOffset(startTime)

            val sleepStage = SleepSessionRecord.Stage(
                startTime = startTime,
                endTime = endTime,
                stage = stage
            )

            val sleepRecord = SleepSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,  // Use system zone offset
                endTime = endTime,
                endZoneOffset = zoneOffset,    // Use system zone offset
                stages = listOf(sleepStage)
            )
            healthConnectClient.insertRecords(listOf(sleepRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing sleep data", e)
        }
    }

    //BMI

    suspend fun readBmiData(startTime: Instant, endTime: Instant): List<LeanBodyMassRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = LeanBodyMassRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.time }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading BMI data", e)
            emptyList()
        }
    }

    suspend fun writeBmiData(height: Float, weight: Float) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val leanBodyMassRecord = LeanBodyMassRecord(
                mass = Mass.kilograms(weight.toDouble()),
                time = currentTime,
                zoneOffset = zoneOffset
            )
            healthConnectClient.insertRecords(listOf(leanBodyMassRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing BMI data", e)
        }
    }

    // Blood Oxygen
    suspend fun readBloodOxygenData(
        startTime: Instant,
        endTime: Instant
    ): List<OxygenSaturationRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = OxygenSaturationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.time }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading blood oxygen data", e)
            emptyList()
        }
    }

    suspend fun writeBloodOxygenData(percentage: Double) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val bloodOxygenRecord = OxygenSaturationRecord(
                percentage = Percentage(percentage)  ,
                time = currentTime,
                zoneOffset = zoneOffset
            )
            healthConnectClient.insertRecords(listOf(bloodOxygenRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing blood oxygen data", e)
        }
    }

    // Blood pressure

    suspend fun readBloodPressureData(startTime: Instant, endTime: Instant): List<BloodPressureRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.time }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading blood pressure data", e)
            emptyList()
        }
    }

    suspend fun writeBloodPressureData(systolic: Double, diastolic: Double) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val bloodPressureRecord = BloodPressureRecord(
                systolic = Pressure.millimetersOfMercury(systolic),
                diastolic = Pressure.millimetersOfMercury(diastolic),
                time = currentTime,
                zoneOffset = zoneOffset
            )
            healthConnectClient.insertRecords(listOf(bloodPressureRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing blood pressure data", e)
        }
    }

    //Respiratory Rate

    suspend fun readRespiratoryData(startTime: Instant, endTime: Instant): List<RespiratoryRateRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = RespiratoryRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.time }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading respiratory rate data", e)
            emptyList()
        }
    }

    suspend fun writeRespiratoryData(rate: Double) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val respiratoryRecord = RespiratoryRateRecord(
                rate = rate,
                time = currentTime,
                zoneOffset = zoneOffset
            )
            healthConnectClient.insertRecords(listOf(respiratoryRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing respiratory rate data", e)
        }
    }

    suspend fun readWorkoutData(startTime: Instant, endTime: Instant): List<ExerciseSessionRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading workout data", e)
            emptyList()
        }
    }

    suspend fun writeWorkoutData(
        startTime: Instant,
        endTime: Instant,
        exerciseType: Int,
        title: String
    ) {
        try {
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)

            val workoutRecord = ExerciseSessionRecord(
                startTime = startTime,
                startZoneOffset = zoneOffset,
                endTime = endTime,
                endZoneOffset = zoneOffset,
                exerciseType = exerciseType,
                title = title
            )
            healthConnectClient.insertRecords(listOf(workoutRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing workout data", e)
        }
    }

    suspend fun readDistanceData(startTime: Instant, endTime: Instant): List<DistanceRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading distance data", e)
            emptyList()
        }
    }

    suspend fun writeDistanceData(
        distance: Double,
        startTime: Instant? = null,
        endTime: Instant? = null
    ) {
        try {
            val currentStart = startTime ?: Instant.now().minus(1, ChronoUnit.HOURS)
            val currentEnd = endTime ?: Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentStart)

            val distanceRecord = DistanceRecord(
                startTime = currentStart,
                startZoneOffset = zoneOffset,
                endTime = currentEnd,
                endZoneOffset = zoneOffset,
                distance = Length.kilometers(distance)
            )
            healthConnectClient.insertRecords(listOf(distanceRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing distance data", e)
        }
    }

    @SuppressLint("RestrictedApi")
    suspend fun readMindfulnessData(startTime: Instant, endTime: Instant): List<MindfulnessSessionRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = MindfulnessSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading mindfulness data", e)
            emptyList()
        }
    }

    @SuppressLint("RestrictedApi")
    suspend fun writeMindfulnessData(
        startTime: Instant? = null,
        endTime: Instant? = null,
        title: String = "Mindfulness Session",
        notes: String? = null,
        mindfulnessType: Int = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
    ) {
        try {
            val currentStart = startTime ?: Instant.now().minus(30, ChronoUnit.MINUTES)
            val currentEnd = endTime ?: Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentStart)

            val mindfulnessRecord = MindfulnessSessionRecord(
                startTime = currentStart,
                startZoneOffset = zoneOffset,
                endTime = currentEnd,
                endZoneOffset = zoneOffset,
                title = title,
                notes = notes,
                mindfulnessSessionType = mindfulnessType,
            )
            healthConnectClient.insertRecords(listOf(mindfulnessRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing mindfulness data", e)
        }
    }



}