package com.demo.healthtracker.bloodpressure


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
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodPressureScreen(
    bloodPressureViewModel: BloodPressureViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    
    val bloodPressureData by bloodPressureViewModel.bloodPressureData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Blood Pressure Reading")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (bloodPressureData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No blood pressure data available")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bloodPressureData) { record ->
                    BloodPressureCard(record, bloodPressureViewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Blood Pressure Reading") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = systolic,
                        onValueChange = { 
                            if (it.isEmpty() || (it.toDoubleOrNull() != null && it.toDouble() <= 300)) {
                                systolic = it 
                            }
                        },
                        label = { Text("Systolic (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = diastolic,
                        onValueChange = { 
                            if (it.isEmpty() || (it.toDoubleOrNull() != null && it.toDouble() <= 200)) {
                                diastolic = it 
                            }
                        },
                        label = { Text("Diastolic (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val systolicValue = systolic.toDoubleOrNull()
                        val diastolicValue = diastolic.toDoubleOrNull()
                        if (systolicValue != null && diastolicValue != null) {
                            bloodPressureViewModel.addBloodPressure(systolicValue, diastolicValue)
                            showAddDialog = false
                            systolic = ""
                            diastolic = ""
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
fun BloodPressureCard(record: BloodPressureRecord, viewModel: BloodPressureViewModel) {
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
                    text = "${record.systolic.inMillimetersOfMercury.toInt()}/${record.diastolic.inMillimetersOfMercury.toInt()} mmHg",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.getBloodPressureCategory(
                        record.systolic.inMillimetersOfMercury,
                        record.diastolic.inMillimetersOfMercury
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = getBloodPressureCategoryColor(
                        record.systolic.inMillimetersOfMercury,
                        record.diastolic.inMillimetersOfMercury
                    )
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

@Composable
private fun getBloodPressureCategoryColor(systolic: Double, diastolic: Double): Color {
    return when {
        systolic < 120 && diastolic < 80 -> Color(0xFF4CAF50) // Green for normal 
        systolic < 130 && diastolic < 80 -> Color(0xFFFFA000) // Orange for elevated
        systolic < 140 || diastolic < 90 -> Color(0xFFF57C00) // Dark Orange for Stage 1
        else -> Color(0xFFF44336)                              // Red for Stage 2
    }
}

private fun formatDateTime(instant: Instant): String {
    return DateTimeFormatter
        .ofPattern("MMM dd, yyyy - hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}