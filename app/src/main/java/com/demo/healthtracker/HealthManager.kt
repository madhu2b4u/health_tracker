package com.demo.healthtracker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class BmiData(
    val time: Instant,
    val height: Double,
    val weight: Double,
    val bmi: Double,
    val bmiCategory: String,
    val leanBodyMass: Double? = null
)



@Singleton
class HealthManager @Inject constructor(
    private val context: Context,
    private val healthConnectClient: HealthConnectClient
) {

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

    suspend fun readTotalStepsForDay(date: LocalDate): Long {
        try {
            // Convert LocalDate to start and end Instant (midnight to midnight)
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            // Read all step records for the day
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Sum up all step counts
            return response.records.sumOf { it.count }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading total steps for day", e)
            return 0L
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
            // Log sleep stages for debugging
            response.records.forEach { record ->
                Log.d("HealthManager", "Sleep session from ${record.startTime} to ${record.endTime}")
                record.stages.forEach { stage ->
                    Log.d("HealthManager", "  Stage: ${getSleepStageString(stage.stage)} from ${stage.startTime} to ${stage.endTime}")
                }
            }

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
            Log.d("HealthManager", "Writing sleep stage: ${getSleepStageString(stage)} from ${startTime} to ${endTime}")


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

    private fun getSleepStageString(stage: Int): String {
        return when (stage) {
            SleepSessionRecord.STAGE_TYPE_AWAKE -> "Awake"
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> "Sleeping"
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "Out of Bed"
            SleepSessionRecord.STAGE_TYPE_LIGHT -> "Light Sleep"
            SleepSessionRecord.STAGE_TYPE_DEEP -> "Deep Sleep"
            SleepSessionRecord.STAGE_TYPE_REM -> "REM Sleep"
            else -> "Unknown"
        }
    }


    suspend fun readRawBmiData(startTime: Instant, endTime: Instant): List<LeanBodyMassRecord> {
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

    /**
     * Read BMI related data from Health Connect
     */
    suspend fun readBmiData(startTime: Instant, endTime: Instant): List<BmiData> {
        try {
            // Read height records
            val heightResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Read weight records
            val weightResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Read lean body mass records
            val leanBodyMassResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = LeanBodyMassRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Get the latest height record or use default
            val latestHeight = heightResponse.records.maxByOrNull { it.time }?.height?.inMeters ?: 1.7

            // Process and combine data to create BMI entries
            val bmiDataList = mutableListOf<BmiData>()

            // Process weight records
            weightResponse.records.forEach { weightRecord ->
                val weight = weightRecord.weight.inKilograms
                val bmi = calculateBmi(weight, latestHeight)

                // Find corresponding lean body mass record if available
                val leanBodyMassRecord = leanBodyMassResponse.records.find {
                    it.time.isClose(weightRecord.time)
                }

                bmiDataList.add(
                    BmiData(
                        time = weightRecord.time,
                        height = latestHeight,
                        weight = weight,
                        bmi = bmi,
                        bmiCategory = getBmiCategory(bmi),
                        leanBodyMass = leanBodyMassRecord?.mass?.inKilograms
                    )
                )
            }

            // Process lean body mass records that don't have corresponding weight records
            leanBodyMassResponse.records
                .filter { lbmRecord ->
                    bmiDataList.none { it.time.isClose(lbmRecord.time) }
                }
                .forEach { lbmRecord ->
                    // Estimate weight based on lean body mass if we don't have actual weight
                    val estimatedWeight = lbmRecord.mass.inKilograms * 1.15 // Rough estimate
                    val bmi = calculateBmi(estimatedWeight, latestHeight)

                    bmiDataList.add(
                        BmiData(
                            time = lbmRecord.time,
                            height = latestHeight,
                            weight = estimatedWeight,
                            bmi = bmi,
                            bmiCategory = getBmiCategory(bmi),
                            leanBodyMass = lbmRecord.mass.inKilograms
                        )
                    )
                }

            return bmiDataList.sortedByDescending { it.time }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading BMI data", e)
            return emptyList()
        }
    }

    /**
     * Write height data to Health Connect
     */
    suspend fun writeHeightData(heightInMeters: Float) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val heightRecord = HeightRecord(
                height = Length.meters(heightInMeters.toDouble()),
                time = currentTime,
                zoneOffset = zoneOffset
            )

            healthConnectClient.insertRecords(listOf(heightRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing height data", e)
        }
    }

    /**
     * Write weight data to Health Connect
     */
    suspend fun writeWeightData(weightInKg: Float) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val weightRecord = WeightRecord(
                weight = Mass.kilograms(weightInKg.toDouble()),
                time = currentTime,
                zoneOffset = zoneOffset
            )

            healthConnectClient.insertRecords(listOf(weightRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing weight data", e)
        }
    }

    /**
     * Write lean body mass data to Health Connect
     */
    suspend fun writeLeanBodyMassData(leanMassInKg: Float) {
        try {
            val currentTime = Instant.now()
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(currentTime)

            val leanBodyMassRecord = LeanBodyMassRecord(
                mass = Mass.kilograms(leanMassInKg.toDouble()),
                time = currentTime,
                zoneOffset = zoneOffset
            )

            healthConnectClient.insertRecords(listOf(leanBodyMassRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing lean body mass data", e)
        }
    }

    /**
     * Write complete BMI data including height, weight, and calculated lean body mass
     */
    suspend fun writeBmiData(heightInMeters: Float, weightInKg: Float, bodyFatPercentage: Float? = null) {
        try {
            writeHeightData(heightInMeters)
            writeWeightData(weightInKg)

            // If body fat percentage is provided, calculate and write lean body mass
            if (bodyFatPercentage != null) {
                val leanBodyMass = weightInKg * (1 - bodyFatPercentage / 100f)
                writeLeanBodyMassData(leanBodyMass)
            } else {
                // Estimate lean body mass using Boer formula if body fat is not provided
                val gender = "male" // This should be a parameter or stored preference
                val leanBodyMass = if (gender == "male") {
                    0.407f * weightInKg + 0.267f * heightInMeters * 100 - 19.2f
                } else {
                    0.252f * weightInKg + 0.473f * heightInMeters * 100 - 48.3f
                }
                writeLeanBodyMassData(leanBodyMass)
            }

        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing BMI data", e)
        }
    }

    /**
     * Calculate BMI from weight and height
     */
    private fun calculateBmi(weightInKg: Double, heightInMeters: Double): Double {
        return weightInKg / (heightInMeters * heightInMeters)
    }

    /**
     * Get BMI category based on BMI value
     */
    private fun getBmiCategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25 -> "Normal"
            bmi < 30 -> "Overweight"
            else -> "Obese"
        }
    }

    /**
     * Extension function to check if two time instants are close to each other
     * (within 1 minute)
     */
    private fun Instant.isClose(other: Instant): Boolean {
        return Math.abs(this.epochSecond - other.epochSecond) < 60
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
                percentage = Percentage(percentage),
                time = currentTime,
                zoneOffset = zoneOffset
            )
            healthConnectClient.insertRecords(listOf(bloodOxygenRecord))
        } catch (e: Exception) {
            Log.e("HealthManager", "Error writing blood oxygen data", e)
        }
    }

    // Blood pressure

    suspend fun readBloodPressureData(
        startTime: Instant,
        endTime: Instant
    ): List<BloodPressureRecord> {
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

    suspend fun readRespiratoryData(
        startTime: Instant,
        endTime: Instant
    ): List<RespiratoryRateRecord> {
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



    suspend fun readTotalDistanceForDay(date: LocalDate): Double {
        try {
            // Convert LocalDate to start and end Instant (midnight to midnight)
            val startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            // Read all distance records for the day
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Sum up all distances, converting to kilometers
            return response.records
                .sumOf { it.distance.inKilometers }
        } catch (e: Exception) {
            Log.e("HealthManager", "Error reading total distance for day", e)
            return 0.0
        }
    }

    // Helper function to get distance data for a range of days
    suspend fun readDailyDistanceData(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Double> {
        val dailyDistances = mutableMapOf<LocalDate, Double>()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val totalDistance = readTotalDistanceForDay(currentDate)
            if (totalDistance > 0) {
                dailyDistances[currentDate] = totalDistance
            }
            currentDate = currentDate.plusDays(1)
        }

        return dailyDistances
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
    suspend fun readMindfulnessData(
        startTime: Instant,
        endTime: Instant
    ): List<MindfulnessSessionRecord> {
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