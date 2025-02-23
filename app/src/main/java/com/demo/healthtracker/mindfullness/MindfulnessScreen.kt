package com.demo.healthtracker.mindfullness

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen() {
    // State variables for the add dialog

    val  mindfulnessViewModel: MindfulnessViewModel = hiltViewModel()
    var showAddDialog by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("Mindfulness Session") }
    var notes by remember { mutableStateOf("") }
    
    // Mindfulness type dropdown
    val mindfulnessTypes = listOf(
        "breathing" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING,
        "meditation" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
        "movement" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT,
        "music" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC,
        "other" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER,
        "unguided" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED
    )
    var expandedTypeDropdown by remember { mutableStateOf(false) }
    var selectedMindfulnessType by remember { 
        mutableStateOf(mindfulnessTypes.first()) 
    }
    
    // Collect mindfulness data from ViewModel
    val mindfulnessData by mindfulnessViewModel.mindfulnessData.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Mindfulness Session")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Mindfulness Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mindfulness Summary",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    // Total Mindfulness Time
                    Text(
                        text = "Total Time: ${formatDuration(mindfulnessViewModel.getTotalMindfulnessTime())}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Average Session Duration
                    Text(
                        text = "Avg. Session: ${formatDuration(mindfulnessViewModel.getAverageMindfulnessSessionDuration())}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Mindfulness Data List
            if (mindfulnessData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No mindfulness sessions recorded")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mindfulnessData) { record ->
                        MindfulnessCard(record)
                    }
                }
            }
        }
    }

    // Add Mindfulness Session Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Mindfulness Session") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duration Input
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { 
                            if (it.isEmpty() || (it.toLongOrNull() != null && it.toLong() <= 120)) {
                                duration = it 
                            }
                        },
                        label = { Text("Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Session Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Mindfulness Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expandedTypeDropdown,
                        onExpandedChange = { expandedTypeDropdown = !expandedTypeDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMindfulnessType.first,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mindfulness Type") },
                            trailingIcon = { 
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandedTypeDropdown
                                ) 
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedTypeDropdown,
                            onDismissRequest = { expandedTypeDropdown = false }
                        ) {
                            mindfulnessTypes.forEach { (type, typeValue) ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedMindfulnessType = type to typeValue
                                        expandedTypeDropdown = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                    
                    // Notes Input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val durationValue = duration.toLongOrNull()
                        if (durationValue != null) {
                            mindfulnessViewModel.addMindfulnessSession(
                                duration = durationValue,
                                title = title.ifEmpty { "Mindfulness Session" },
                                notes = notes.ifEmpty { null },
                                mindfulnessType = selectedMindfulnessType.second
                            )
                            showAddDialog = false
                            // Reset input fields
                            duration = ""
                            title = "Mindfulness Session"
                            notes = ""
                            selectedMindfulnessType = mindfulnessTypes.first()
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

@SuppressLint("RestrictedApi")
@Composable
fun MindfulnessCard(@SuppressLint("RestrictedApi") record: MindfulnessSessionRecord) {
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
            // Main content row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Title
                    Text(
                        text = record.title.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Duration
                    Text(
                        text = "Duration: ${formatDuration(Duration.between(record.startTime, record.endTime))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Mindfulness Type
                    Text(
                        text = "Type: ${getMindfulnessTypeName(record.mindfulnessSessionType)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Notes (if available)
            record.notes?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Timestamp
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDateTime(record.startTime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Utility function to format duration
private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return when {
        hours > 0 -> "${hours}h ${remainingMinutes}m"
        else -> "${minutes}m"
    }
}

// Utility function to format date and time
private fun formatDateTime(instant: Instant): String {
    return DateTimeFormatter
        .ofPattern("MMM dd, yyyy - hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}

// Utility function to get mindfulness type name
@SuppressLint("RestrictedApi")
private fun getMindfulnessTypeName(type: Int): String {
    return when (type) {
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING -> "Breathing"
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION -> "Meditation"
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT -> "Movement"
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC -> "Music"
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER -> "Other"
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED -> "Unguided"
        MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN -> "Unknown"
        else -> "Unknown"
    }
}