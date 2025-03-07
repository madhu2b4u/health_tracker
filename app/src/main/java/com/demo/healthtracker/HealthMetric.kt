package com.demo.healthtracker

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.demo.healthtracker.service2.TAG
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
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class to represent health metrics in a standardized format
 */
data class HealthMetric(
    val intervalEndDate: String,
    val elementType: String,
    val elementName: String,
    val intervalStartDate: String,
    val source: String,
    val unit: String,
    val value: String,
    val isUserManual: Boolean
)

/**
 * Data class to wrap a list of health metrics with metadata
 */
data class HealthMetricsResult(
    val metrics: List<HealthMetric>,
    val startTime: Instant,
    val endTime: Instant,
    val count: Int,
    val metricTypes: List<String> = emptyList(),
    val sources: List<String> = emptyList()
)

/**
 * Health monitoring service that continuously monitors health data
 * and provides flexible access to health metrics
 */
@Singleton
class HealthDataMonitorV3 @Inject constructor(
    private val healthManager: HealthManager,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    private var lastRefreshTime = 0L

    // Using ISO 8601 format with timezone
    private val dateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        .withZone(ZoneId.systemDefault())

    // Use decimal format to ensure proper formatting of numeric values
    private val decimalFormat = DecimalFormat("0.0")

    // Default device name when the source is unknown
    private val defaultDeviceName = "Android Device"

    // Health metrics flow that can be collected
    private val _healthMetrics = MutableStateFlow<List<HealthMetric>>(emptyList())
    val healthMetrics: StateFlow<List<HealthMetric>> = _healthMetrics.asStateFlow()

    private var changesToken: String? = null

    /**
     * Start monitoring health data from connected devices
     */
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

    /**
     * Public method to refresh data with throttling
     */
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

    /**
     * Format an Instant to ISO 8601 string with timezone
     */
    private fun formatToIso8601(time: Instant): String {
        return dateTimeFormatter.format(time)
    }

    /**
     * Get device name from metadata or return default
     */
    private fun getDeviceName(metadata: Metadata): String {
        // Health Connect uses Metadata class instead of Map<String, String>
        // We need to extract the device name from the metadata if available
        return metadata?.device?.let { device ->
            device.manufacturer?.let { mfg ->
                val model = device.model ?: "Device"
                "$mfg $model"
            }
        } ?: defaultDeviceName
    }

    /**
     * Check if a record was manually entered
     */
    private fun isManualEntry(metadata: Metadata): Boolean {
        // Check if the record was manually entered
        // This depends on how your HealthManager implementation marks manual entries
        return metadata?.dataOrigin?.packageName == "com.demo.healthtracker" ||
                metadata?.dataOrigin?.packageName?.contains("manual") == true
    }

    /**
     * Refresh health data with default time range (30 days)
     */
    private fun refreshHealthData() {
        // Default to last 30 days
        val endTime = Instant.now()
        val startTime = endTime.minus(30, ChronoUnit.DAYS)
        refreshHealthData(startTime, endTime)
    }

    /**
     * Refresh health data for a specific time range
     */
    fun refreshHealthData(startTime: Instant, endTime: Instant = Instant.now()) {
        scope.launch {
            try {
                Log.i(TAG, "========== STARTING HEALTH DATA REFRESH ==========")
                Log.i(TAG, "Fetching data from ${formatToIso8601(startTime)} to ${formatToIso8601(endTime)}")

                val metrics = mutableListOf<HealthMetric>()

                // Process steps data
                Log.d(TAG, "Fetching steps data...")
                val stepsRecords = healthManager.readStepsData(startTime, endTime)
                Log.i(TAG, "Found ${stepsRecords.size} step records")

                stepsRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    Log.d(TAG, "Step record: ${record.count} steps from ${formatToIso8601(record.startTime)} to ${formatToIso8601(record.endTime)} from $deviceName (manual: $isManual)")

                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.endTime),
                            elementType = "",
                            elementName = "stepcount",
                            intervalStartDate = formatToIso8601(record.startTime),
                            source = deviceName,
                            unit = "count",
                            value = decimalFormat.format(record.count),
                            isUserManual = isManual
                        )
                    )
                }
                // Process heart rate data
                Log.d(TAG, "Fetching heart rate data...")
                val heartRateRecords = healthManager.readHeartRateData(startTime, endTime)
                Log.i(TAG, "Found ${heartRateRecords.size} heart rate records with ${heartRateRecords.sumOf { it.samples.size }} samples")

                heartRateRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)

                    record.samples.forEach { sample ->
                        Log.d(TAG, "Heart rate sample: ${sample.beatsPerMinute} bpm at ${formatToIso8601(sample.time)} from $deviceName (manual: $isManual)")

                        metrics.add(
                            HealthMetric(
                                intervalEndDate = formatToIso8601(sample.time),
                                elementType = "",
                                elementName = "heartrate",
                                intervalStartDate = formatToIso8601(sample.time),
                                source = deviceName,
                                unit = "bpm",
                                value = decimalFormat.format(sample.beatsPerMinute),
                                isUserManual = isManual
                            )
                        )
                    }
                }

                // Process blood pressure data
                Log.d(TAG, "Fetching blood pressure data...")
                val bpRecords = healthManager.readBloodPressureData(startTime, endTime)
                Log.i(TAG, "Found ${bpRecords.size} blood pressure records")

                bpRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    Log.d(TAG, "Blood pressure record: ${record.systolic.inMillimetersOfMercury.toInt()}/${record.diastolic.inMillimetersOfMercury.toInt()} mmHg at ${formatToIso8601(record.time)} from $deviceName (manual: $isManual)")

                    // Systolic
                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.time),
                            elementType = "",
                            elementName = "bloodpressure_systolic",
                            intervalStartDate = formatToIso8601(record.time),
                            source = deviceName,
                            unit = "mmHg",
                            value = decimalFormat.format(record.systolic.inMillimetersOfMercury),
                            isUserManual = isManual
                        )
                    )

                    // Diastolic
                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.time),
                            elementType = "",
                            elementName = "bloodpressure_diastolic",
                            intervalStartDate = formatToIso8601(record.time),
                            source = deviceName,
                            unit = "mmHg",
                            value = decimalFormat.format(record.diastolic.inMillimetersOfMercury),
                            isUserManual = isManual
                        )
                    )
                }

                // Process blood oxygen data
                Log.d(TAG, "Fetching blood oxygen data...")
                val spo2Records = healthManager.readBloodOxygenData(startTime, endTime)
                Log.i(TAG, "Found ${spo2Records.size} blood oxygen records")

                spo2Records.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    val oxygenPercentage = record.percentage.value * 100
                    Log.d(TAG, "Blood oxygen record: ${oxygenPercentage}% at ${formatToIso8601(record.time)} from $deviceName (manual: $isManual)")

                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.time),
                            elementType = "",
                            elementName = "bloodoxygen",
                            intervalStartDate = formatToIso8601(record.time),
                            source = deviceName,
                            unit = "percentage",
                            value = decimalFormat.format(oxygenPercentage),
                            isUserManual = isManual
                        )
                    )
                }

                // Process respiratory rate data
                Log.d(TAG, "Fetching respiratory rate data...")
                val respiratoryRecords = healthManager.readRespiratoryData(startTime, endTime)
                Log.i(TAG, "Found ${respiratoryRecords.size} respiratory rate records")

                respiratoryRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    Log.d(TAG, "Respiratory rate record: ${record.rate} breaths/min at ${formatToIso8601(record.time)} from $deviceName (manual: $isManual)")

                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.time),
                            elementType = "",
                            elementName = "respiratoryrate",
                            intervalStartDate = formatToIso8601(record.time),
                            source = deviceName,
                            unit = "breaths/min",
                            value = decimalFormat.format(record.rate),
                            isUserManual = isManual
                        )
                    )
                }

                // Process workout data
                Log.d(TAG, "Fetching workout data...")
                val workoutRecords = healthManager.readWorkoutData(startTime, endTime)
                Log.i(TAG, "Found ${workoutRecords.size} workout records")

                workoutRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    val durationMinutes = Duration.between(record.startTime, record.endTime).toMinutes()
                    Log.d(TAG, "Workout record: ${record.exerciseType} for $durationMinutes minutes from ${formatToIso8601(record.startTime)} to ${formatToIso8601(record.endTime)} from $deviceName (manual: $isManual)")

                    // Duration
                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.endTime),
                            elementType = record.exerciseType.toString(),
                            elementName = "workout",
                            intervalStartDate = formatToIso8601(record.startTime),
                            source = deviceName,
                            unit = "minutes",
                            value = decimalFormat.format(durationMinutes),
                            isUserManual = isManual
                        )
                    )
                    // need to work on workout
                }

                // Process sleep data
                Log.d(TAG, "Fetching sleep data...")
                val sleepRecords = healthManager.readSleepData(startTime, endTime)
                Log.i(TAG, "Found ${sleepRecords.size} sleep records")

                sleepRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    val durationHours = Duration.between(record.startTime, record.endTime).toMinutes() / 60.0
                    Log.d(TAG, "Sleep record: ${durationHours} hours from ${formatToIso8601(record.startTime)} to ${formatToIso8601(record.endTime)} from $deviceName (manual: $isManual)")

                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.endTime),
                            elementType = "",
                            elementName = "sleep",
                            intervalStartDate = formatToIso8601(record.startTime),
                            source = deviceName,
                            unit = "hours",
                            value = decimalFormat.format(durationHours),
                            isUserManual = isManual
                        )
                    )
                }

                // Process BMI data
                Log.d(TAG, "Fetching BMI data...")
                val bmiRecords = healthManager.readBmiData(startTime, endTime)
                Log.i(TAG, "Found ${bmiRecords.size} BMI records")

                bmiRecords.forEach { record ->
                    // Extract device name and manual entry status
                    // Note: Since we're combining data from multiple sources, we use a generic source
                    val deviceName = "Health Connect"
                    val isManual = false // We would need more complex logic to determine this correctly

                    // Log details
                    Log.d(TAG, "BMI record: Weight ${record.weight} kg, BMI ${record.bmi} at ${formatToIso8601(record.time)}")

                    // Add weight metric
                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.time),
                            elementType = "",
                            elementName = "weight",
                            intervalStartDate = formatToIso8601(record.time),
                            source = deviceName,
                            unit = "kg",
                            value = decimalFormat.format(record.weight),
                            isUserManual = isManual
                        )
                    )

                    // Add BMI metric
                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.time),
                            elementType = record.bmiCategory,
                            elementName = "bmi",
                            intervalStartDate = formatToIso8601(record.time),
                            source = deviceName,
                            unit = "kg/m²",
                            value = decimalFormat.format(record.bmi),
                            isUserManual = isManual
                        )
                    )

                    // Add lean body mass metric if available
                    record.leanBodyMass?.let { lbm ->
                        metrics.add(
                            HealthMetric(
                                intervalEndDate = formatToIso8601(record.time),
                                elementType = "",
                                elementName = "leanbodymass",
                                intervalStartDate = formatToIso8601(record.time),
                                source = deviceName,
                                unit = "kg",
                                value = decimalFormat.format(lbm),
                                isUserManual = isManual
                            )
                        )
                    }
                }

                // Process distance data
                Log.d(TAG, "Fetching distance data...")
                val distanceRecords = healthManager.readDistanceData(startTime, endTime)
                Log.i(TAG, "Found ${distanceRecords.size} distance records")

                distanceRecords.forEach { record ->
                    val deviceName = getDeviceName(record.metadata)
                    val isManual = isManualEntry(record.metadata)
                    Log.d(TAG, "Distance record: ${record.distance.inKilometers} km from ${formatToIso8601(record.startTime)} to ${formatToIso8601(record.endTime)} from $deviceName (manual: $isManual)")

                    metrics.add(
                        HealthMetric(
                            intervalEndDate = formatToIso8601(record.endTime),
                            elementType = "",
                            elementName = "distance",
                            intervalStartDate = formatToIso8601(record.startTime),
                            source = deviceName,
                            unit = "km",
                            value = decimalFormat.format(record.distance.inKilometers),
                            isUserManual = isManual
                        )
                    )
                }

                // Update metrics flow
                val oldMetricsCount = _healthMetrics.value.size
                _healthMetrics.value = metrics
                Log.i(TAG, "Updated metrics from $oldMetricsCount to ${metrics.size} records")

                // Log summary statistics
                val metricTypes = metrics.groupBy { it.elementName }
                Log.i(TAG, "Health metric summary:")
                metricTypes.forEach { (type, records) ->
                    Log.i(TAG, "- $type: ${records.size} records")
                }

                // Log sample metrics for major types (if available)
                val sampleTypes = listOf("stepcount", "heartrate", "leanbodymass", "sleep", "distance")
                sampleTypes.forEach { type ->
                    metricTypes[type]?.firstOrNull()?.let { sample ->
                        Log.i(TAG, "Sample $type: ${sample.value} ${sample.unit} at ${sample.intervalStartDate}")
                    }
                }

                Log.i(TAG, "Health data refresh completed with ${metrics.size} total metrics")
                Log.i(TAG, "========== HEALTH DATA REFRESH COMPLETE ==========")
                val metricsUpdats = _healthMetrics.value
                metricsUpdats

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing health data", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Stop monitoring health data
     */
    fun stopMonitoring() {
        if (monitoringJob != null) {
            Log.i("HealthMonitor", "Stopping health monitoring...")
            monitoringJob?.cancel()
            monitoringJob = null
            Log.i("HealthMonitor", "Health monitoring stopped successfully")
        }
    }


    /**
     * Extension function to check if two timestamps are close to each other (within 5 minutes)
     */
    private fun Instant.isClose(other: Instant): Boolean {
        return Duration.between(this, other).abs().toMinutes() < 5
    }

    /**
     * Gets all health metrics
     * @return List of HealthMetric objects
     */
    fun getAllHealthMetrics(): List<HealthMetric> {
        return _healthMetrics.value
    }

    /**
     * Gets health metrics within a specific time range
     * @param startTimestamp Starting timestamp as Instant
     * @param endTimestamp Ending timestamp as Instant (defaults to current time)
     * @return List of HealthMetric objects within the specified time range
     */
    fun getHealthMetricsByTimeRange(
        startTimestamp: Instant,
        endTimestamp: Instant = Instant.now()
    ): List<HealthMetric> {
        // Parse the dates from the HealthMetric objects
        return _healthMetrics.value.filter { metric ->
            try {
                val metricStartInstant = Instant.from(dateTimeFormatter.parse(metric.intervalStartDate))
                val metricEndInstant = Instant.from(dateTimeFormatter.parse(metric.intervalEndDate))

                // Check if metric falls within the specified range
                // A metric is included if its time interval overlaps with the requested range
                (metricStartInstant.isAfter(startTimestamp) || metricStartInstant.equals(startTimestamp)) &&
                        (metricEndInstant.isBefore(endTimestamp) || metricEndInstant.equals(endTimestamp))
            } catch (e: Exception) {
                Log.e("HealthMetrics", "Error parsing date in metric filter", e)
                false // Exclude records with invalid dates
            }
        }
    }

    /**
     * Gets health metrics within a specific time range and returns a result object
     * @param startTimestamp Starting timestamp as Instant
     * @param endTimestamp Ending timestamp as Instant (defaults to current time)
     * @return HealthMetricsResult containing the metrics and metadata
     */
    fun getHealthMetricsResultByTimeRange(
        startTimestamp: Instant,
        endTimestamp: Instant = Instant.now()
    ): HealthMetricsResult {
        val filteredMetrics = getHealthMetricsByTimeRange(startTimestamp, endTimestamp)

        return HealthMetricsResult(
            metrics = filteredMetrics,
            startTime = startTimestamp,
            endTime = endTimestamp,
            count = filteredMetrics.size,
            metricTypes = filteredMetrics.map { it.elementName }.distinct(),
            sources = filteredMetrics.map { it.source }.distinct()
        )
    }

    /**
     * Gets health metrics for the last 24 hours
     * @return HealthMetricsResult containing the metrics and metadata
     */
    fun getLastDayMetrics(): HealthMetricsResult {
        val now = Instant.now()
        val oneDayAgo = now.minus(1, ChronoUnit.DAYS)

        return getHealthMetricsResultByTimeRange(oneDayAgo, now)
    }

    /**
     * Gets health metrics for the last week
     * @return HealthMetricsResult containing the metrics and metadata
     */
    fun getLastWeekMetrics(): HealthMetricsResult {
        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)

        return getHealthMetricsResultByTimeRange(oneWeekAgo, now)
    }

    /**
     * Gets health metrics by metric type within a specific time range
     * @param metricType Type of metric (e.g., "heartrate", "stepcount", etc.)
     * @param startTimestamp Starting timestamp as Instant
     * @param endTimestamp Ending timestamp as Instant (defaults to current time)
     * @return List of HealthMetric objects of the specified type and within the time range
     */
    fun getHealthMetricsByTypeAndTimeRange(
        metricType: String,
        startTimestamp: Instant,
        endTimestamp: Instant = Instant.now()
    ): List<HealthMetric> {
        return getHealthMetricsByTimeRange(startTimestamp, endTimestamp)
            .filter { it.elementName == metricType }
    }

    /**
     * Helper function to convert a string timestamp to Instant
     * @param timestamp ISO 8601 formatted timestamp string
     * @return Instant object or null if parsing fails
     */
    fun parseTimestamp(timestamp: String): Instant? {
        return try {
            Instant.from(dateTimeFormatter.parse(timestamp))
        } catch (e: Exception) {
            Log.e("HealthMetrics", "Error parsing timestamp: $timestamp", e)
            null
        }
    }

    /**
     * Retrieves health metrics filtered by multiple criteria
     * @param startTime Optional starting time to filter metrics
     * @param endTime Optional ending time to filter metrics (defaults to current time)
     * @param metricTypes Optional list of metric types to include (e.g., ["heartrate", "stepcount"])
     * @param sources Optional list of sources to include (e.g., ["Fitbit Sense", "Manual Entry"])
     * @param includeManual Whether to include manually entered data (default: true)
     * @return List of filtered HealthMetric objects
     */
    fun getFilteredHealthMetrics(
        startTime: Instant? = null,
        endTime: Instant? = Instant.now(),
        metricTypes: List<String>? = null,
        sources: List<String>? = null,
        includeManual: Boolean = true
    ): List<HealthMetric> {
        // Create a copy of the current metrics list
        var filteredMetrics = _healthMetrics.value.toList()

        // Filter by time range if provided
        if (startTime != null || endTime != null) {
            filteredMetrics = filteredMetrics.filter { metric ->
                try {
                    val metricStartInstant = Instant.from(dateTimeFormatter.parse(metric.intervalStartDate))
                    val metricEndInstant = Instant.from(dateTimeFormatter.parse(metric.intervalEndDate))

                    // Check start time constraint
                    val afterStart = startTime?.let {
                        metricStartInstant.isAfter(it) || metricStartInstant.equals(it)
                    } ?: true

                    // Check end time constraint
                    val beforeEnd = endTime?.let {
                        metricEndInstant.isBefore(it) || metricEndInstant.equals(it)
                    } ?: true

                    afterStart && beforeEnd
                } catch (e: Exception) {
                    Log.e("HealthMetrics", "Error parsing date in advanced filter", e)
                    false
                }
            }
        }

        // Filter by metric types if provided
        if (metricTypes != null) {
            filteredMetrics = filteredMetrics.filter { metric ->
                metricTypes.contains(metric.elementName)
            }
        }

        // Filter by sources if provided
        if (sources != null) {
            filteredMetrics = filteredMetrics.filter { metric ->
                sources.contains(metric.source)
            }
        }

        // Filter by manual entry status if needed
        if (!includeManual) {
            filteredMetrics = filteredMetrics.filter { !it.isUserManual }
        }

        return filteredMetrics
    }

    /**
     * Get available metric types from the current data
     * @return List of unique metric types (element names)
     */
    fun getAvailableMetricTypes(): List<String> {
        return _healthMetrics.value.map { it.elementName }.distinct()
    }

    /**
     * Get available data sources from the current data
     * @return List of unique data sources
     */
    fun getAvailableSources(): List<String> {
        return _healthMetrics.value.map { it.source }.distinct()
    }

    /**
     * Get metrics grouped by date
     * @param startTime Starting time to filter metrics (optional)
     * @param endTime Ending time to filter metrics (defaults to current time)
     * @return Map of date string to list of metrics for that date
     */
    fun getMetricsGroupedByDate(
        startTime: Instant? = null,
        endTime: Instant? = Instant.now()
    ): Map<String, List<HealthMetric>> {
        // Get filtered metrics
        val metrics = getFilteredHealthMetrics(startTime, endTime)

        // Group by date part of the start date
        return metrics.groupBy { metric ->
            // Extract just the date part (YYYY-MM-DD)
            metric.intervalStartDate.split("T")[0]
        }
    }

    /**
     * Get daily summaries for a specific metric type
     * @param metricType Type of metric (e.g., "stepcount")
     * @param startTime Starting time (optional)
     * @param endTime Ending time (defaults to current time)
     * @return Map of date string to summary value
     */
    fun getDailySummary(
        metricType: String,
        startTime: Instant? = null,
        endTime: Instant? = Instant.now()
    ): Map<String, Double> {
        // Get metrics of the specified type
        val metrics = getFilteredHealthMetrics(
            startTime = startTime,
            endTime = endTime,
            metricTypes = listOf(metricType)
        )

        // Group by date
        val groupedByDate = metrics.groupBy { metric ->
            metric.intervalStartDate.split("T")[0]
        }

        // Create summaries based on metric type
        val result = mutableMapOf<String, Double>()

        for ((date, dateMetrics) in groupedByDate) {
            when (metricType) {
                // For cumulative metrics like steps, sum the values
                "stepcount", "calories", "distance", "workout_distance" -> {
                    result[date] = dateMetrics.sumOf { it.value.toDoubleOrNull() ?: 0.0 }
                }

                // For metrics like heart rate, calculate average
                "heartrate", "bloodpressure_systolic", "bloodpressure_diastolic",
                "bloodoxygen", "respiratoryrate" -> {
                    val values = dateMetrics.mapNotNull { it.value.toDoubleOrNull() }
                    if (values.isNotEmpty()) {
                        result[date] = values.average()
                    }
                }

                // For duration metrics like sleep, take the sum
                "sleep", "workout" -> {
                    result[date] = dateMetrics.sumOf { it.value.toDoubleOrNull() ?: 0.0 }
                }

                // For measurement metrics like weight or BMI, take the last reading of the day
                "leanbodymass" -> {
                    val sortedMetrics = dateMetrics.sortedByDescending { it.intervalEndDate }
                    if (sortedMetrics.isNotEmpty()) {
                        result[date] = sortedMetrics.first().value.toDoubleOrNull() ?: 0.0
                    }
                }

                // Default case - just sum the values
                else -> {
                    result[date] = dateMetrics.sumOf { it.value.toDoubleOrNull() ?: 0.0 }
                }
            }
        }

        return result
    }

    /**
     * Get a detailed breakdown of metrics for a specific time period
     * @param period Time period to analyze ("day", "week", "month", "year")
     * @param targetDate Reference date (defaults to current date)
     * @return HealthMetricsResult containing metrics and metadata
     */
    fun getMetricsForPeriod(
        period: String,
        targetDate: Instant = Instant.now()
    ): HealthMetricsResult {
        val endTime = targetDate
        val startTime = when (period.lowercase()) {
            "day" -> targetDate.minus(1, ChronoUnit.DAYS)
            "week" -> targetDate.minus(7, ChronoUnit.DAYS)
            "month" -> targetDate.minus(30, ChronoUnit.DAYS)
            "year" -> targetDate.minus(365, ChronoUnit.DAYS)
            else -> targetDate.minus(1, ChronoUnit.DAYS) // Default to day
        }

        val metrics = getFilteredHealthMetrics(startTime, endTime)

        return HealthMetricsResult(
            metrics = metrics,
            startTime = startTime,
            endTime = endTime,
            count = metrics.size,
            metricTypes = metrics.map { it.elementName }.distinct(),
            sources = metrics.map { it.source }.distinct()
        )
    }

    /**
     * Refresh health data for a specific period
     * @param period Time period to fetch ("day", "week", "month", "year")
     */
    fun refreshHealthDataForPeriod(period: String) {
        val endTime = Instant.now()
        val startTime = when (period.lowercase()) {
            "day" -> endTime.minus(1, ChronoUnit.DAYS)
            "week" -> endTime.minus(7, ChronoUnit.DAYS)
            "month" -> endTime.minus(30, ChronoUnit.DAYS)
            "year" -> endTime.minus(365, ChronoUnit.DAYS)
            else -> endTime.minus(30, ChronoUnit.DAYS) // Default to month
        }

        refreshHealthData(startTime, endTime)
    }

    /**
     * Get a single metric type with the most recent value
     * @param metricType Type of metric (e.g., "heartrate", "stepcount")
     * @return Most recent HealthMetric of the specified type or null if not found
     */
    fun getMostRecentMetric(metricType: String): HealthMetric? {
        return _healthMetrics.value
            .filter { it.elementName == metricType }
            .maxByOrNull { parseTimestamp(it.intervalEndDate) ?: Instant.EPOCH }
    }

    /**
     * Get the most recent value for each metric type
     * @return Map of metric type to most recent HealthMetric
     */
    fun getMostRecentMetrics(): Map<String, HealthMetric> {
        return _healthMetrics.value
            .groupBy { it.elementName }
            .mapValues { (_, metrics) ->
                metrics.maxByOrNull { parseTimestamp(it.intervalEndDate) ?: Instant.EPOCH }!!
            }
    }

    /**
     * Group metrics by source device
     * @param startTime Optional starting time to filter metrics
     * @param endTime Optional ending time to filter metrics (defaults to current time)
     * @return Map of source device to list of metrics
     */
    fun getMetricsBySource(
        startTime: Instant? = null,
        endTime: Instant? = Instant.now()
    ): Map<String, List<HealthMetric>> {
        val filteredMetrics = getFilteredHealthMetrics(startTime, endTime)
        return filteredMetrics.groupBy { it.source }
    }

    /**
     * Get metrics for a specific day
     * @param date The date to fetch metrics for
     * @return List of HealthMetric objects for the specified day
     */
    fun getMetricsForDay(date: Instant): List<HealthMetric> {
        val startOfDay = date.truncatedTo(ChronoUnit.DAYS)
        val endOfDay = startOfDay.plus(1, ChronoUnit.DAYS).minusNanos(1)

        return getHealthMetricsByTimeRange(startOfDay, endOfDay)
    }

    /**
     * Get a summary of health metrics for dashboard display
     * @return Map of metric type to summary value
     */
    fun getHealthDashboardSummary(): Map<String, Any> {
        val now = Instant.now()
        val yesterday = now.minus(1, ChronoUnit.DAYS)

        val recentMetrics = getFilteredHealthMetrics(yesterday, now)
        val summary = mutableMapOf<String, Any>()

        // Get step count for today
        val todaySteps = recentMetrics
            .filter {
                it.elementName == "stepcount" &&
                        parseTimestamp(it.intervalStartDate)?.truncatedTo(ChronoUnit.DAYS) == now.truncatedTo(ChronoUnit.DAYS)
            }
            .sumOf { it.value.toDoubleOrNull() ?: 0.0 }

        summary["todaySteps"] = todaySteps

        // Get most recent heart rate
        val latestHeartRate = getMostRecentMetric("heartrate")
        if (latestHeartRate != null) {
            summary["latestHeartRate"] = latestHeartRate.value.toDoubleOrNull() ?: 0.0
            summary["heartRateTime"] = latestHeartRate.intervalEndDate
        }

        // Get sleep from last night
        val lastSleep = recentMetrics
            .filter { it.elementName == "sleep" }
            .maxByOrNull { parseTimestamp(it.intervalEndDate) ?: Instant.EPOCH }

        if (lastSleep != null) {
            summary["lastSleepHours"] = lastSleep.value.toDoubleOrNull() ?: 0.0
            summary["sleepEndTime"] = lastSleep.intervalEndDate
        }

        // Get most recent blood pressure if available
        val latestSystolic = getMostRecentMetric("bloodpressure_systolic")
        val latestDiastolic = getMostRecentMetric("bloodpressure_diastolic")

        if (latestSystolic != null && latestDiastolic != null) {
            summary["latestBloodPressure"] = "${latestSystolic.value}/${latestDiastolic.value}"
            summary["bloodPressureTime"] = latestSystolic.intervalEndDate
        }

        // Add BMI and weight information
        val latestBmi = getMostRecentMetric("bmi")
        val latestWeight = getMostRecentMetric("weight")

        if (latestBmi != null) {
            summary["latestBmi"] = latestBmi.value.toDoubleOrNull() ?: 0.0
            summary["bmiCategory"] = latestBmi.elementType
            summary["bmiTime"] = latestBmi.intervalEndDate
        }

        if (latestWeight != null) {
            summary["latestWeight"] = latestWeight.value.toDoubleOrNull() ?: 0.0
            summary["weightTime"] = latestWeight.intervalEndDate
        }

        return summary
    }

    /**
     * Data classes for BMI history and analysis
     */
    data class BmiRecord(
        val time: Instant,
        val value: Double,
        val category: String
    )

    data class WeightRecord(
        val time: Instant,
        val value: Double
    )
}