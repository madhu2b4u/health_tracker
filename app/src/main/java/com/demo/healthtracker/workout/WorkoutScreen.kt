package com.demo.healthtracker.workout


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    workoutViewModel: WorkoutViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    val workoutData by workoutViewModel.workoutData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Workout")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (workoutData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No workout data available")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workoutData) { record ->
                    WorkoutCard(record, workoutViewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AddWorkoutDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { startTime, endTime, exerciseType, title ->
                workoutViewModel.addWorkout(startTime, endTime, exerciseType, title)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun WorkoutCard(record: ExerciseSessionRecord, viewModel: WorkoutViewModel) {
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
                    text = record.title.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = viewModel.getDuration(record.startTime, record.endTime),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Started: ${formatDateTime(record.startTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Ended: ${formatDateTime(record.endTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutDialog(
    onDismiss: () -> Unit,
    onAdd: (LocalDateTime, LocalDateTime, Int, String) -> Unit
) {
    var selectedStartDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedStartTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedEndDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedEndTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedExerciseType by remember { mutableStateOf(0) }
    var workoutTitle by remember { mutableStateOf("") }

    val exerciseTypes = listOf(
        "Running" to ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        "Walking" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        "Hiking" to ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        "Biking" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        "Yoga" to ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        "Swimming" to ExerciseSessionRecord.EXERCISE_TYPE_SAILING,
        "Workout" to ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Workout") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = workoutTitle,
                    onValueChange = { workoutTitle = it },
                    label = { Text("Workout Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { },
                ) {
                    OutlinedTextField(
                        value = exerciseTypes.find { it.second == selectedExerciseType }?.first ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Exercise Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier.menuAnchor()
                    )

                    DropdownMenu(
                        expanded = false,
                        onDismissRequest = { },
                    ) {
                        exerciseTypes.forEach { (name, type) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedExerciseType = type }
                            )
                        }
                    }
                }

                // Date and Time pickers implementation would go here
                // Similar to what we did in previous implementations
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startDateTime = LocalDateTime.of(selectedStartDate, selectedStartTime)
                    val endDateTime = LocalDateTime.of(selectedEndDate, selectedEndTime)
                    if (workoutTitle.isNotEmpty() && endDateTime.isAfter(startDateTime)) {
                        onAdd(startDateTime, endDateTime, selectedExerciseType, workoutTitle)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDateTime(instant: Instant): String {
    return DateTimeFormatter
        .ofPattern("MMM dd, yyyy - hh:mm a")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}