package com.demo.healthtracker.service3

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.HealthDataMonitorV3
import com.demo.healthtracker.HealthMetric
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDataScreen(
    onBackClick: () -> Unit,
    viewModel: HealthDataViewModel = hiltViewModel()
) {
    val healthData by viewModel.healthData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedMetricType = remember { mutableStateOf("all") }
    val availableMetricTypes by viewModel.availableMetricTypes.collectAsState()
    val dailySummaries by viewModel.dailySummaries.collectAsState()
    
    // Initialize data fetch
    LaunchedEffect(key1 = true) {
        viewModel.loadLast30DaysData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Data - Last 30 Days") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metric type selector
            if (availableMetricTypes.isNotEmpty()) {
                ScrollableMetricTabs(
                    metricTypes = listOf("all") + availableMetricTypes,
                    selectedMetricType = selectedMetricType.value,
                    onMetricTypeSelected = { selectedMetricType.value = it }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (healthData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No health data available.\nTry refreshing or check your health data sources.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Display data by date
                HealthDataList(
                    healthData = healthData,
                    selectedMetricType = selectedMetricType.value,
                    dailySummaries = dailySummaries
                )
            }
        }
    }
}

@Composable
fun ScrollableMetricTabs(
    metricTypes: List<String>,
    selectedMetricType: String,
    onMetricTypeSelected: (String) -> Unit
) {
    val formattedMetricTypes = metricTypes.map { type ->
        when (type) {
            "all" -> "All"
            "stepcount" -> "Steps"
            "heartrate" -> "Heart Rate"
            "bloodpressure_systolic" -> "BP (Systolic)"
            "bloodpressure_diastolic" -> "BP (Diastolic)"
            "bloodoxygen" -> "Blood Oxygen"
            "sleep" -> "Sleep"
            "workout" -> "Workouts"
            "distance" -> "Distance"
            "bmi" -> "BMI"
            "weight" -> "Weight"
            "leanbodymass" -> "Lean Mass"
            "respiratoryrate" -> "Resp. Rate"
            else -> type.replaceFirstChar { it.uppercase() }
        }
    }

    TabRow(
        selectedTabIndex = metricTypes.indexOf(selectedMetricType),
        modifier = Modifier.fillMaxWidth()
    ) {
        metricTypes.forEachIndexed { index, type ->
            Tab(
                selected = selectedMetricType == type,
                onClick = { onMetricTypeSelected(type) },
                text = { Text(formattedMetricTypes[index]) }
            )
        }
    }
}

@Composable
fun HealthDataList(
    healthData: List<HealthMetric>,
    selectedMetricType: String,
    dailySummaries: Map<String, Map<String, Double>>
) {
    // Group data by date
    val groupedData = if (selectedMetricType == "all") {
        healthData.groupBy {
            val date = it.intervalStartDate.split("T")[0]
            date
        }
    } else {
        healthData.filter { it.elementName == selectedMetricType }
            .groupBy {
                val date = it.intervalStartDate.split("T")[0]
                date
            }
    }

    // Sort dates in reverse order (newest first)
    val sortedDates = groupedData.keys.sorted().reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        items(sortedDates) { date ->
            DateGroup(
                date = date,
                metrics = groupedData[date] ?: emptyList(),
                selectedMetricType = selectedMetricType,
                dailySummaries = dailySummaries[date] ?: emptyMap()
            )
        }
    }
}

@Composable
fun DateGroup(
    date: String,
    metrics: List<HealthMetric>,
    selectedMetricType: String,
    dailySummaries: Map<String, Double>
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val localDate = LocalDate.parse(date, dateFormatter)
    val formattedDate = localDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Date",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // If we're showing all metrics or a specific one
            if (selectedMetricType == "all") {
                // Group by metric type
                val metricsByType = metrics.groupBy { it.elementName }
                
                // Special handling for steps and distance - show daily totals
                val stepsMetrics = metricsByType["stepcount"]
                val distanceMetrics = metricsByType["distance"]
                
                // Show daily steps if available
                if (stepsMetrics != null && dailySummaries.containsKey("stepcount")) {
                    val dailySteps = dailySummaries["stepcount"] ?: 0.0
                    DailySummaryMetricItem(
                        title = "Steps",
                        value = dailySteps.toInt().toString(),
                        unit = "steps",
                        icon = "👣"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Show daily distance if available
                if (distanceMetrics != null && dailySummaries.containsKey("distance")) {
                    val dailyDistance = dailySummaries["distance"] ?: 0.0
                    val formattedDistance = "%.2f".format(dailyDistance)
                    DailySummaryMetricItem(
                        title = "Distance",
                        value = formattedDistance,
                        unit = "km",
                        icon = "🏃"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Show other metrics
                metricsByType.forEach { (type, typeMetrics) ->
                    // Skip steps and distance as they're handled above
                    if (type != "stepcount" && type != "distance") {
                        when (type) {
                            "heartrate" -> {
                                val avgHeartRate = typeMetrics.mapNotNull { it.value.toDoubleOrNull() }.average()
                                val formattedHeartRate = "%.1f".format(avgHeartRate)
                                MetricSummaryItem(
                                    title = "Heart Rate",
                                    value = formattedHeartRate,
                                    unit = "bpm",
                                    icon = "❤️",
                                    count = typeMetrics.size
                                )
                            }
                            "sleep" -> {
                                if (typeMetrics.isNotEmpty()) {
                                    val sleep = typeMetrics.first()
                                    MetricItem(metric = sleep, displayName = "Sleep")
                                }
                            }
                            "bloodpressure_systolic" -> {
                                if (typeMetrics.isNotEmpty()) {
                                    val systolicMetric = typeMetrics.maxByOrNull { it.intervalEndDate }!!
                                    val diastolicMetrics = metrics.filter { it.elementName == "bloodpressure_diastolic" }
                                    val diastolicMetric = diastolicMetrics.maxByOrNull { it.intervalEndDate }
                                    
                                    if (diastolicMetric != null) {
                                        BloodPressureItem(
                                            systolic = systolicMetric,
                                            diastolic = diastolicMetric
                                        )
                                    } else {
                                        MetricItem(metric = systolicMetric, displayName = "Blood Pressure (Systolic)")
                                    }
                                }
                            }
                            "bloodoxygen" -> {
                                val avgOxygen = typeMetrics.mapNotNull { it.value.toDoubleOrNull() }.average()
                                val formattedOxygen = "%.1f".format(avgOxygen)
                                MetricSummaryItem(
                                    title = "Blood Oxygen",
                                    value = formattedOxygen,
                                    unit = "%",
                                    icon = "🫁",
                                    count = typeMetrics.size
                                )
                            }
                            "weight" -> {
                                if (typeMetrics.isNotEmpty()) {
                                    val weight = typeMetrics.maxByOrNull { it.intervalEndDate }!!
                                    MetricItem(metric = weight, displayName = "Weight")
                                }
                            }
                            "bmi" -> {
                                if (typeMetrics.isNotEmpty()) {
                                    val bmi = typeMetrics.maxByOrNull { it.intervalEndDate }!!
                                    MetricItem(metric = bmi, displayName = "BMI")
                                }
                            }
                            "workout" -> {
                                typeMetrics.forEach { workout ->
                                    MetricItem(
                                        metric = workout,
                                        displayName = "Workout (${workout.elementType})"
                                    )
                                }
                            }
                            "bloodpressure_diastolic" -> {
                                // Skip, handled with systolic
                            }
                            else -> {
                                typeMetrics.forEach { metric ->
                                    MetricItem(
                                        metric = metric,
                                        displayName = type.replaceFirstChar { it.uppercase() }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Show selected metric details
                when (selectedMetricType) {
                    "stepcount" -> {
                        if (dailySummaries.containsKey("stepcount")) {
                            val dailySteps = dailySummaries["stepcount"] ?: 0.0
                            DailySummaryMetricItem(
                                title = "Daily Steps",
                                value = dailySteps.toInt().toString(),
                                unit = "steps",
                                icon = "👣"
                            )
                        } else {
                            metrics.forEach { metric ->
                                MetricItem(metric = metric, displayName = "Steps")
                            }
                        }
                    }
                    "distance" -> {
                        if (dailySummaries.containsKey("distance")) {
                            val dailyDistance = dailySummaries["distance"] ?: 0.0
                            val formattedDistance = "%.2f".format(dailyDistance)
                            DailySummaryMetricItem(
                                title = "Daily Distance",
                                value = formattedDistance,
                                unit = "km",
                                icon = "🏃"
                            )
                        } else {
                            metrics.forEach { metric ->
                                MetricItem(metric = metric, displayName = "Distance")
                            }
                        }
                    }
                    "heartrate" -> {
                        val avgHeartRate = metrics.mapNotNull { it.value.toDoubleOrNull() }.average()
                        val minHeartRate = metrics.mapNotNull { it.value.toDoubleOrNull() }.minOrNull() ?: 0.0
                        val maxHeartRate = metrics.mapNotNull { it.value.toDoubleOrNull() }.maxOrNull() ?: 0.0
                        
                        HeartRateDetailItem(
                            avg = avgHeartRate,
                            min = minHeartRate,
                            max = maxHeartRate,
                            count = metrics.size
                        )
                        
                        // Optionally show individual readings
                        metrics.take(5).forEach { metric ->
                            MetricItem(metric = metric, displayName = "Heart Rate")
                        }
                        
                        if (metrics.size > 5) {
                            Text(
                                text = "... and ${metrics.size - 5} more readings",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        metrics.forEach { metric ->
                            MetricItem(
                                metric = metric,
                                displayName = selectedMetricType.replaceFirstChar { it.uppercase() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricItem(metric: HealthMetric, displayName: String) {
    val time = metric.intervalEndDate.split("T")[1].split("+")[0]
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (metric.elementName) {
                "stepcount" -> "👣"
                "heartrate" -> "❤️"
                "bloodpressure_systolic", "bloodpressure_diastolic" -> "🩸"
                "bloodoxygen" -> "🫁"
                "respiratoryrate" -> "🫀"
                "sleep" -> "😴"
                "workout" -> "🏋️"
                "distance" -> "🏃"
                "weight" -> "⚖️"
                "bmi" -> "📊"
                "leanbodymass" -> "💪"
                else -> "📝"
            }
            Text(text = icon)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                Text(
                    text = "${metric.value} ${metric.unit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (metric.elementName == "bmi" && metric.elementType.isNotEmpty()) {
                    Text(
                        text = " (${metric.elementType})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Text(
                text = "at $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (metric.isUserManual) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Manual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun HeartRateDetailItem(avg: Double, min: Double, max: Double, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❤️",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Heart Rate Summary",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.1f".format(avg),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Avg bpm",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.0f".format(min),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Min bpm",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.0f".format(max),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Max bpm",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$count measurement${if (count > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BloodPressureItem(systolic: HealthMetric, diastolic: HealthMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🩸")
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Blood Pressure",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${systolic.value}/${diastolic.value} mmHg",
                style = MaterialTheme.typography.bodyMedium
            )
            
            val time = systolic.intervalEndDate.split("T")[1].split("+")[0]
            Text(
                text = "at $time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (systolic.isUserManual) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Manual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun MetricSummaryItem(title: String, value: String, unit: String, icon: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "$value $unit (avg of $count readings)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun DailySummaryMetricItem(title: String, value: String, unit: String, icon: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Daily total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}