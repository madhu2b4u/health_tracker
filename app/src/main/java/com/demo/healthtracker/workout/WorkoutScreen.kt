package com.demo.healthtracker.workout

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.formatDateTime
import com.demo.healthtracker.sleep.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
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
                record.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = getExerciseTypeName(record.exerciseType),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Duration: ${viewModel.getDuration(record.startTime, record.endTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
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

fun getExerciseTypeName(type: Int): String {
    return when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Biking"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Swimming Pool"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming Open Water"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "Workout"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dancing"
        ExerciseSessionRecord.EXERCISE_TYPE_BOXING -> "Boxing"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weight Lifting"
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "Calisthenics"
        ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS -> "Exercise Class"
        else -> "Unknown Exercise"
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
    var selectedExerciseType by remember { mutableStateOf(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING) }
    var workoutTitle by remember { mutableStateOf("") }
    var showExerciseTypeMenu by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }


    val exerciseTypes = listOf(
        "Running" to ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        "Walking" to ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        "Hiking" to ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        "Biking" to ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        "Swimming Pool" to ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        "Swimming Open Water" to ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
        "Workout" to ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        "Yoga" to ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
        "Dancing" to ExerciseSessionRecord.EXERCISE_TYPE_DANCING,
        "Boxing" to ExerciseSessionRecord.EXERCISE_TYPE_BOXING,
        "Weight Lifting" to ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        "Calisthenics" to ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        "Exercise Class" to ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS
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
                    expanded = showExerciseTypeMenu,
                    onExpandedChange = { showExerciseTypeMenu = it }
                ) {
                    OutlinedTextField(
                        value = exerciseTypes.find { it.second == selectedExerciseType }?.first
                            ?: "Select Exercise",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exercise Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showExerciseTypeMenu) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showExerciseTypeMenu,
                        onDismissRequest = { showExerciseTypeMenu = false }
                    ) {
                        exerciseTypes.forEach { (name, type) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedExerciseType = type
                                    showExerciseTypeMenu = false
                                }
                            )
                        }
                    }
                }

                Text("Start Time", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { showStartDatePicker = true }) {
                        Text(selectedStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    }
                    OutlinedButton(onClick = { showStartTimePicker = true }) {
                        Text(selectedStartTime.format(DateTimeFormatter.ofPattern("hh:mm a")))
                    }
                }

                Text("End Time", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { showEndDatePicker = true }) {
                        Text(selectedEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    }
                    OutlinedButton(onClick = { showEndTimePicker = true }) {
                        Text(selectedEndTime.format(DateTimeFormatter.ofPattern("hh:mm a")))
                    }
                }

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

    // Date Pickers
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedStartDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedStartDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartDatePicker = false }
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedEndDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedEndDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showEndDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndDatePicker = false }
                ) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    // Time Pickers
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                selectedStartTime = LocalTime.of(hour, minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                selectedEndTime = LocalTime.of(hour, minute)
                showEndTimePicker = false
            }
        )
    }
}
