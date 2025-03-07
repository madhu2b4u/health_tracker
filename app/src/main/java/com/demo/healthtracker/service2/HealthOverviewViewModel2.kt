package com.demo.healthtracker.service2

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.demo.healthtracker.service.HealthDataMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val TAG = "HealthMonitor"


@HiltViewModel
@RequiresApi(Build.VERSION_CODES.S)
class HealthOverviewViewModel2 @Inject constructor(
    private val healthDataMonitor: HealthDataMonitorV2
) : ViewModel() {

    // Expose the health data flow from the monitor
    val healthData = healthDataMonitor.healthData

    init {
        // Ensure monitoring is started when the ViewModel is created
        healthDataMonitor.startMonitoring()
    }

    // Public method to refresh data
    fun refreshData() {
        healthDataMonitor.refreshData()
    }

    // Helper functions to format data for display
    fun formatSteps(record: StepsRecord?): String {
        return record?.count?.toString() ?: "No data"
    }

    fun formatHeartRate(record: HeartRateRecord?): String {
        return record?.samples?.firstOrNull()?.beatsPerMinute?.toString() ?: "No data"
    }

    fun formatBloodPressure(record: BloodPressureRecord?): String {
        return if (record != null) {
            "${record.systolic.inMillimetersOfMercury.toInt()}/${record.diastolic.inMillimetersOfMercury.toInt()} mmHg"
        } else {
            "No data"
        }
    }

    fun formatBloodOxygen(record: OxygenSaturationRecord?): String {
        return if (record != null) {
            "${record.percentage.value.toInt()}%"
        } else {
            "No data"
        }
    }

    fun formatRespiratoryRate(record: RespiratoryRateRecord?): String {
        return if (record != null) {
            "${record.rate.toInt()} breaths/min"
        } else {
            "No data"
        }
    }

    fun formatWeight(record: LeanBodyMassRecord?): String {
        return if (record != null) {
            String.format("%.1f kg", record.mass.inKilograms)
        } else {
            "No data"
        }
    }
    fun formatDate(date: LocalDate): String {
        return DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").format(date)
    }
    fun formatDateTime(time: Instant?): String {
        return if (time != null) {
            DateTimeFormatter
                .ofPattern("MMM dd, HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(time)
        } else {
            ""
        }
    }

    fun getDuration(start: Instant?, end: Instant?): String {
        if (start == null || end == null) return ""

        val duration = Duration.between(start, end)
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatSleepStages(record: SleepSessionRecord?): String {
        if (record == null || record.stages.isEmpty()) return "No stage data"

        val stageMap = mutableMapOf<Int, Duration>()

        record.stages.forEach { stage ->
            val duration = Duration.between(stage.startTime, stage.endTime)
            val currentDuration = stageMap.getOrDefault(stage.stage, Duration.ZERO)
            stageMap[stage.stage] = currentDuration.plus(duration)
        }

        return stageMap.entries.joinToString("\n") { (stage, duration) ->
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            val timeStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            "${getSleepStageString(stage)}: $timeStr"
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
}
