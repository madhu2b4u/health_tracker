package com.demo.healthtracker.workout

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    
    private val healthManager = HealthManager(context)
    
    private val _workoutData = MutableStateFlow<List<ExerciseSessionRecord>>(emptyList())
    val workoutData: StateFlow<List<ExerciseSessionRecord>> = _workoutData.asStateFlow()

    init {
        loadWorkoutData()
    }

    private fun loadWorkoutData() {
        viewModelScope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(7, ChronoUnit.DAYS)
            _workoutData.value = healthManager.readWorkoutData(startTime, endTime)
        }
    }

    fun addWorkout(startTime: LocalDateTime, endTime: LocalDateTime, exerciseType: Int, title: String) {
        viewModelScope.launch {
            val systemZone = ZoneId.systemDefault()
            healthManager.writeWorkoutData(
                startTime = startTime.atZone(systemZone).toInstant(),
                endTime = endTime.atZone(systemZone).toInstant(),
                exerciseType = exerciseType,
                title = title
            )
            loadWorkoutData()
        }
    }

    @SuppressLint("NewApi")
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
}