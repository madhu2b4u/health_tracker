package com.demo.healthtracker.service3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A reusable card component for displaying health metrics
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    subtitle: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, style = MaterialTheme.typography.titleMedium)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A card component for displaying step counts
 */
@Composable
fun StepsCard(
    steps: Int,
    goal: Int = 10000,
    modifier: Modifier = Modifier
) {
    val progressPercentage = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val progressText = "${(progressPercentage * 100).toInt()}% of daily goal"
    
    MetricCard(
        title = "Steps",
        value = steps.toString(),
        unit = "steps",
        icon = "👣",
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.secondary,
        subtitle = progressText
    )
}

/**
 * A card component for displaying heart rate
 */
@Composable
fun HeartRateCard(
    heartRate: Double,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Heart Rate",
        value = "%.0f".format(heartRate),
        unit = "bpm",
        icon = "❤️",
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.errorContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.error
    )
}

/**
 * A card component for displaying blood pressure
 */
@Composable
fun BloodPressureCard(
    systolic: Int,
    diastolic: Int,
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Blood Pressure",
        value = "$systolic/$diastolic",
        unit = "mmHg",
        icon = "🩸",
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.tertiary
    )
}

/**
 * A card component for displaying sleep duration
 */
@Composable
fun SleepCard(
    hours: Double,
    modifier: Modifier = Modifier
) {
    val formattedHours = "%.1f".format(hours)
    val hoursInt = hours.toInt()
    val minutesInt = ((hours - hoursInt) * 60).toInt()
    val timeText = "$hoursInt h $minutesInt min"
    
    MetricCard(
        title = "Sleep",
        value = formattedHours,
        unit = "hours",
        icon = "😴",
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        iconBackgroundColor = MaterialTheme.colorScheme.primary,
        subtitle = timeText
    )
}

/**
 * A card component for displaying weight
 */
@Composable
fun WeightCard(
    weight: Double,
    unit: String = "kg",
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Weight",
        value = "%.1f".format(weight),
        unit = unit,
        icon = "⚖️",
        modifier = modifier
    )
}

/**
 * A card component for displaying distance
 */
@Composable
fun DistanceCard(
    distance: Double,
    unit: String = "km",
    modifier: Modifier = Modifier
) {
    MetricCard(
        title = "Distance",
        value = "%.2f".format(distance),
        unit = unit,
        icon = "🏃",
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    )
}