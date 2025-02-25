package com.demo.healthtracker.mindfullness

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/*
    The warning “Restricted API” appears because MindfulnessSessionRecord is still marked as an experimental API in Health Connect.

🔍 Why is this happening?
	•	Google sometimes marks certain APIs as restricted when they are in early development or internal testing.
	•	Health Connect’s MindfulnessSessionRecord might not be fully stable yet.
 */

@HiltViewModel
@SuppressLint("RestrictedApi")
class MindfulnessViewModel @Inject constructor(
    private val healthManager: HealthManager
) : ViewModel() {

    private val _mindfulnessData = MutableStateFlow<List<MindfulnessSessionRecord>>(emptyList())
    val mindfulnessData: StateFlow<List<MindfulnessSessionRecord>> = _mindfulnessData.asStateFlow()

    init {
        loadMindfulnessData()
    }

    private fun loadMindfulnessData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _mindfulnessData.value = healthManager.readMindfulnessData(startTime, endTime)
        }
    }

    fun addMindfulnessSession(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        title: String,
        notes: String?,
        mindfulnessType: Int
    ) {
        viewModelScope.launch {
            val systemZone = ZoneId.systemDefault()
            healthManager.writeMindfulnessData(
                startTime = startTime.atZone(systemZone).toInstant(),
                endTime = endTime.atZone(systemZone).toInstant(),
                title = title,
                notes = notes,
                mindfulnessType = mindfulnessType
            )
            loadMindfulnessData()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun getDuration(startTime: Instant, endTime: Instant): String {
        val duration = Duration.between(startTime, endTime)
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun getMindfulnessTypeName(type: Int): String {
        return when (type) {
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING -> "Breathing"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION -> "Meditation"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT -> "Movement"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC -> "Music"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED -> "Unguided"
            MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER -> "Other"
            else -> "Unknown"
        }
    }
}