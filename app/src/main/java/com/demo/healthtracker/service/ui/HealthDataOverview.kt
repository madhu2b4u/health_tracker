package com.demo.healthtracker.service.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.formatDateTime
import com.demo.healthtracker.formatDuration
import com.demo.healthtracker.formatTime


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HealthDataOverview(viewModel: HealthViewModel = hiltViewModel()) {
    val healthData by viewModel.healthData.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Health Overview",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        healthData?.let { data ->
            // Steps
            data.steps.firstOrNull()?.let { steps ->
                item {
                    HealthMetricCard(
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        title = "Steps",
                        value = "${steps.count} steps",
                        time = formatDateTime(steps.startTime)
                    )
                }
            }

            // Heart Rate
            data.heartRate.firstOrNull()?.let { heartRate ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Favorite,
                        title = "Heart Rate",
                        value = "${heartRate.samples.firstOrNull()?.beatsPerMinute ?: 0} BPM",
                        time = formatDateTime(heartRate.startTime)
                    )
                }
            }

            // Blood Pressure
            data.bloodPressure.firstOrNull()?.let { bp ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.MonitorHeart,
                        title = "Blood Pressure",
                        value = "${bp.systolic.inMillimetersOfMercury.toInt()}/${bp.diastolic.inMillimetersOfMercury.toInt()} mmHg",
                        time = formatDateTime(bp.time)
                    )
                }
            }

            // Blood Oxygen
            data.bloodOxygen.firstOrNull()?.let { oxygen ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Bloodtype,
                        title = "Blood Oxygen",
                        value = "${oxygen.percentage.value}%",
                        time = formatDateTime(oxygen.time)
                    )
                }
            }

            // Respiratory Rate
            data.respiratory.firstOrNull()?.let { respiratory ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Air,
                        title = "Respiratory Rate",
                        value = "${respiratory.rate} breaths/min",
                        time = formatDateTime(respiratory.time)
                    )
                }
            }

            // Latest Workout
            data.workout.firstOrNull()?.let { workout ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.FitnessCenter,
                        title = "Latest Workout",
                        value = workout.title.toString(),
                        time = formatDateTime(workout.startTime),
                        subtitle = "Duration: ${formatDuration(workout.startTime, workout.endTime)}"
                    )
                }
            }

            // Latest Sleep
            data.sleep.firstOrNull()?.let { sleep ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Bedtime,
                        title = "Sleep",
                        value = formatDuration(sleep.startTime, sleep.endTime),
                        time = formatDateTime(sleep.startTime),
                        subtitle = "From: ${formatTime(sleep.startTime)} To: ${formatTime(sleep.endTime)}"
                    )
                }
            }
        }
    }
}

@Composable
fun HealthMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    time: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Last updated: $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
