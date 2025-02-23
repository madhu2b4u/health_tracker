package com.demo.healthtracker.mindfullness

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.HealthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class MindfulnessViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val healthManager = HealthManager(context)
    
    private val _mindfulnessData = MutableStateFlow<List<MindfulnessSessionRecord>>(emptyList())
    @SuppressLint("RestrictedApi")
    val mindfulnessData: StateFlow<List<MindfulnessSessionRecord>> = _mindfulnessData.asStateFlow()

    init {
        loadMindfulnessData()
    }

    private fun loadMindfulnessData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(30, ChronoUnit.DAYS)
            _mindfulnessData.value = healthManager.readMindfulnessData(startTime, endTime)
        }
    }

    fun addMindfulnessSession(
        duration: Long,
        title: String = "Mindfulness Session",
        notes: String? = null,
        @SuppressLint("RestrictedApi") mindfulnessType: Int = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
    ) {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(duration, ChronoUnit.MINUTES)
            healthManager.writeMindfulnessData(
                startTime = startTime,
                endTime = endTime,
                title = title,
                notes = notes,
                mindfulnessType = mindfulnessType
            )
            loadMindfulnessData()
        }
    }

    @SuppressLint("RestrictedApi")
    fun getTotalMindfulnessTime(): Duration {
        return _mindfulnessData.value.fold(Duration.ZERO) { acc, record ->
            acc.plus(Duration.between(record.startTime, record.endTime))
        }
    }

    fun getAverageMindfulnessSessionDuration(): Duration {
        val sessions = _mindfulnessData.value
        return if (sessions.isNotEmpty()) {
            getTotalMindfulnessTime().dividedBy(sessions.size.toLong())
        } else {
            Duration.ZERO
        }
    }
}