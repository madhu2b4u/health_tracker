package com.demo.healthtracker.bmi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.healthtracker.formatDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import android.util.Log
import androidx.compose.material3.Slider
import com.demo.healthtracker.BmiData
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.pow


@Composable
fun BmiScreen(
    viewModel: BMIViewModel = hiltViewModel()
) {
    val bmiData by viewModel.bmiData.collectAsState()
    val height by viewModel.selectedHeight.collectAsState()
    val weight by viewModel.selectedWeight.collectAsState()
    val bodyFatPercentage by viewModel.bodyFatPercentage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "BMI Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Height input
        Text(
            text = "Height (meters)",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = height,
            onValueChange = { viewModel.updateHeight(it) },
            valueRange = 1.0f..2.2f,
            steps = 120,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = String.format("%.2f m", height),
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Weight input
        Text(
            text = "Weight (kg)",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = weight,
            onValueChange = { viewModel.updateWeight(it) },
            valueRange = 30f..150f,
            steps = 120,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = String.format("%.1f kg", weight),
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Optional body fat percentage input
        Text(
            text = "Body Fat % (optional)",
            style = MaterialTheme.typography.bodyLarge
        )

        var bodyFatText by remember { mutableStateOf(bodyFatPercentage?.toString() ?: "") }

        OutlinedTextField(
            value = bodyFatText,
            onValueChange = {
                bodyFatText = it
                viewModel.updateBodyFatPercentage(it.toFloatOrNull())
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Body Fat %") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // BMI Results
        val bmi = viewModel.calculateBmi()
        val bmiCategory = viewModel.getBmiCategory()
        val leanBodyMass = viewModel.calculateLeanBodyMass()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format("BMI: %.1f", bmi),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Category: $bmiCategory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = when(bmiCategory) {
                        "Underweight" -> Color(0xFF2196F3)
                        "Normal" -> Color(0xFF4CAF50)
                        "Slightly Overweight" -> Color(0xFFFFC107)
                        "Overweight" -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )

                Text(
                    text = String.format("Estimated Lean Body Mass: %.1f kg", leanBodyMass),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save button
        Button(
            onClick = { viewModel.saveBmiData() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save to Health Connect")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History
        Text(
            text = "History",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(bmiData) { entry ->
                BmiHistoryItem(entry)
            }
        }
    }
}

@Composable
fun BmiHistoryItem(data: BmiData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Format date for display
                val dateFormatter = DateTimeFormatter
                    .ofPattern("MMM dd, yyyy")
                    .withZone(ZoneId.systemDefault())

                Text(
                    text = dateFormatter.format(data.time),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = String.format("BMI: %.1f - %s", data.bmi, data.bmiCategory),
                    style = MaterialTheme.typography.bodySmall
                )

                data.leanBodyMass?.let {
                    Text(
                        text = String.format("Lean Mass: %.1f kg", it),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.1f kg", data.weight),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = String.format("%.2f m", data.height),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun getBmiCategoryColor(bmi: Double): Color {
    // Since we're working with lean mass instead of BMI directly,
    // we need to estimate the BMI value
    val estimatedTotalWeight = bmi / 0.85  // Assuming lean mass is ~85% of total weight
    val estimatedBmi = estimatedTotalWeight / (1.7 * 1.7)  // Assuming average height of 1.7m

    return when {
        estimatedBmi < 18.5 -> Color(0xFF2196F3) // Blue for underweight
        estimatedBmi < 25 -> Color(0xFF4CAF50)   // Green for normal
        estimatedBmi < 30 -> Color(0xFFFFA000)   // Orange for overweight
        else -> Color(0xFFF44336)       // Red for obese
    }
}