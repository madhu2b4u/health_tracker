package com.demo.healthtracker.bloodoxygen

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
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodOxygenScreen(
    bloodOxygenViewModel: BloodOxygenViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var percentage by remember { mutableStateOf("") }

    val bloodOxygenData by bloodOxygenViewModel.bloodOxygenData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Blood Oxygen Reading")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bloodOxygenData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No blood oxygen data available")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bloodOxygenData) { record ->
                    BloodOxygenCard(record, bloodOxygenViewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Blood Oxygen Reading") },
            text = {
                Column {
                    OutlinedTextField(
                        value = percentage,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() || char == '.' }
                            if (filtered.isEmpty() || filtered.toDoubleOrNull()!! in 0.0..100.0) {
                                percentage = filtered
                            }
                        },
                        label = { Text("SpO2 (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        percentage.toDoubleOrNull()?.let { value ->
                            if (value in 0.0..100.0) {
                                bloodOxygenViewModel.addBloodOxygen(value)
                                showAddDialog = false
                                percentage = ""
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BloodOxygenCard(record: OxygenSaturationRecord, viewModel: BloodOxygenViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%.1f%%", record.percentage.value), // Access the value property
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.getOxygenCategory(record.percentage.value), // Access the value property
                    style = MaterialTheme.typography.bodyLarge,
                    color = getOxygenCategoryColor(record.percentage.value) // Access the value property
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDateTime(record.time),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Also update the ViewModel's function
fun getOxygenCategory(percentage: Double): String {
    return when {
        percentage >= 95.0 -> "Normal"
        percentage >= 90.0 -> "Mild Hypoxemia"
        percentage >= 85.0 -> "Moderate Hypoxemia"
        else -> "Severe Hypoxemia"
    }
}

@Composable
private fun getOxygenCategoryColor(percentage: Double): Color {
    return when {
        percentage >= 95.0 -> Color(0xFF4CAF50) // Green for normal
        percentage >= 90.0 -> Color(0xFFFFA000) // Orange for mild
        percentage >= 85.0 -> Color(0xFFF57C00) // Dark Orange for moderate
        else -> Color(0xFFF44336)               // Red for severe
    }
}

private fun formatDateTime(instant: Instant): String {
    return DateTimeFormatter
        .ofPattern("MMM dd, yyyy - hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}