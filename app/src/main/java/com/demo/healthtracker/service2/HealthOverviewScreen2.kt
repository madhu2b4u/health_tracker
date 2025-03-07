package com.demo.healthtracker.service2


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.formatDateTime
import com.demo.healthtracker.formatDuration
import com.demo.healthtracker.formatTime
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HealthOverviewScreen2(
    viewModel: HealthOverviewViewModel2 = hiltViewModel()
) {
    val healthData by viewModel.healthData.collectAsState()

    // Refresh data when screen becomes active
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Set up a periodic refresh as a fallback
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Check every minute as a fallback
            viewModel.refreshData()
        }
    }

    fun formatDate(date: LocalDate): String {
        return DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").format(date)
    }

    if (healthData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("Loading health data...")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Latest Health Metrics",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    "Last updated: ${
                        DateTimeFormatter
                            .ofPattern("MMM dd, yyyy - HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Activity Metrics
            item {
                CategoryHeader("Activity")
            }


            // Steps
            healthData?.steps?.firstOrNull()?.let { steps ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.DirectionsWalk,
                        title = "Steps",
                        value = viewModel.formatSteps(steps),
                        time = viewModel.formatDate(steps.startTime.atZone(ZoneId.systemDefault()).toLocalDate()),
                        )
                }
            }

            // Distance
            healthData?.distance?.firstOrNull()?.let { distance ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.SocialDistance,
                        title = "Daily Distance",
                        value = String.format("%.2f km", distance.distance.inKilometers),
                        time = viewModel.formatDate(distance.startTime.atZone(ZoneId.systemDefault()).toLocalDate()),
                    )
                }
            }

            // Latest Workout
            healthData?.workout?.firstOrNull()?.let { workout ->
                item {
                    workout.title?.let {
                        HealthMetricCard(
                            icon = Icons.Default.FitnessCenter,
                            title = "Latest Workout",
                            value = it,
                            time = viewModel.formatDateTime(workout.startTime),
                            subtitle = "Duration: ${viewModel.getDuration(workout.startTime, workout.endTime)}"
                        )
                    }
                }
            }

            // Vitals
            item {
                CategoryHeader("Vitals")
            }

            // Heart Rate
            healthData?.heartRate?.firstOrNull()?.let { heartRate ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Favorite,
                        title = "Heart Rate",
                        value = "${viewModel.formatHeartRate(heartRate)} BPM",
                        time = viewModel.formatDateTime(heartRate.startTime)
                    )
                }
            }

            // Blood Pressure
            healthData?.bloodPressure?.firstOrNull()?.let { bloodPressure ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.MonitorHeart,
                        title = "Blood Pressure",
                        value = viewModel.formatBloodPressure(bloodPressure),
                        time = viewModel.formatDateTime(bloodPressure.time)
                    )
                }
            }

            // Blood Oxygen
            healthData?.bloodOxygen?.firstOrNull()?.let { bloodOxygen ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Bloodtype,
                        title = "Blood Oxygen",
                        value = viewModel.formatBloodOxygen(bloodOxygen),
                        time = viewModel.formatDateTime(bloodOxygen.time)
                    )
                }
            }

            // Respiratory Rate
            healthData?.respiratory?.firstOrNull()?.let { respiratory ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Air,
                        title = "Respiratory Rate",
                        value = viewModel.formatRespiratoryRate(respiratory),
                        time = viewModel.formatDateTime(respiratory.time)
                    )
                }
            }

            // Body Measurements
            item {
                CategoryHeader("Body")
            }

            // Weight/BMI
            healthData?.bmi?.firstOrNull()?.let { bmi ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Scale,
                        title = "Weight",
                        value = viewModel.formatWeight(bmi),
                        time = viewModel.formatDateTime(bmi.time)
                    )
                }
            }

            // Sleep & Wellness
            item {
                CategoryHeader("Sleep & Wellness")
            }

            // Sleep
            healthData?.sleep?.firstOrNull()?.let { sleep ->
                item {
                    HealthMetricCard(
                        icon = Icons.Default.Bedtime,
                        title = "Sleep",
                        value = viewModel.getDuration(sleep.startTime, sleep.endTime),
                        time = viewModel.formatDateTime(sleep.startTime),
                        subtitle = viewModel.formatSleepStages(sleep),
                        expandable = true // Make this card expandable due to potentially long content
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun HealthMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    time: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    expandable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }


    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
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

                if (time.isNotEmpty()) {
                    Text(
                        text = "Last updated: $time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expandable && !subtitle.isNullOrEmpty()) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Show less" else "Show more"
                    )
                }
            }
            if (subtitle != null) {
                if (!expandable || expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}