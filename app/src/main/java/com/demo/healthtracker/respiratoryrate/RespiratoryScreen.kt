package com.demo.healthtracker.respiratoryrate


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
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.formatDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RespiratoryScreen(
    respiratoryViewModel: RespiratoryViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var rate by remember { mutableStateOf("") }

    val respiratoryData by respiratoryViewModel.respiratoryData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Respiratory Rate")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (respiratoryData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No respiratory rate data available")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(respiratoryData) { record ->
                    RespiratoryCard(record, respiratoryViewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Respiratory Rate") },
            text = {
                Column {
                    OutlinedTextField(
                        value = rate,
                        onValueChange = {
                            if (it.isEmpty() || (it.toDoubleOrNull() != null && it.toDouble() <= 60)) {
                                rate = it
                            }
                        },
                        label = { Text("Breaths per minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        rate.toDoubleOrNull()?.let { value ->
                            respiratoryViewModel.addRespiratoryRate(value)
                            showAddDialog = false
                            rate = ""
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
fun RespiratoryCard(record: RespiratoryRateRecord, viewModel: RespiratoryViewModel) {
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
                    text = "${record.rate.toInt()} BPM",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.getRateCategory(record.rate),
                    style = MaterialTheme.typography.bodyLarge,
                    color = getRateCategoryColor(record.rate)
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
private fun getRateCategoryColor(rate: Double): Color {
    return when {
        rate < 12 -> Color(0xFFF44336)  // Red for below normal
        rate <= 20 -> Color(0xFF4CAF50) // Green for normal
        else -> Color(0xFFF44336)       // Red for above normal
    }
}
