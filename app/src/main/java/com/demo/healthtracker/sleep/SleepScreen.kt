package com.demo.healthtracker.sleep

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.getDurationText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen() {

    val sleepViewModel: SleepViewModel = hiltViewModel()

    var showAddDialog by remember { mutableStateOf(false) }

    val sleepData by sleepViewModel.sleepData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sleep History") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Sleep Session")
                    }
                }
            )
        }
    ) { padding ->
        if (sleepData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No sleep data available")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(sleepData) { record ->
                    SleepCard(record)
                }
            }
        }

        if (showAddDialog) {
            AddSleepDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { start, end, stage ->
                    sleepViewModel.addSleepSession(
                        startTime = start,
                        endTime = end,
                        stage = stage
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SleepCard(record: SleepSessionRecord) {
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    }

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
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getDurationText(record.startTime, record.endTime),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display sleep stages
            Text(
                text = "Sleep Stages:",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            ) {
                if (record.stages.isEmpty()) {
                    Text(
                        text = "No stage data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    record.stages.forEach { stage ->
                        val stageName = getSleepStageString(stage.stage)
                        val stageDuration = getDurationText(stage.startTime, stage.endTime)

                        Text(
                            text = "$stageName: $stageDuration",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "From: ${
                    record.startTime.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
                }",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "To: ${
                    record.endTime.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
                }",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Add this helper function to your file
fun getSleepStageString(stage: Int): String {
    return when (stage) {
        SleepSessionRecord.STAGE_TYPE_AWAKE -> "Awake"
        SleepSessionRecord.STAGE_TYPE_SLEEPING -> "Sleeping"
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "Out of Bed"
        SleepSessionRecord.STAGE_TYPE_LIGHT -> "Light Sleep"
        SleepSessionRecord.STAGE_TYPE_DEEP -> "Deep Sleep"
        SleepSessionRecord.STAGE_TYPE_REM -> "REM Sleep"
        else -> "Unknown"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSleepDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime, LocalDateTime, Int) -> Unit
) {
    var selectedStartDate by remember {
        mutableStateOf(
            LocalDate.now().minusDays(1)
        )
    }
    var selectedStartTime by remember { mutableStateOf(LocalTime.of(22, 0)) }
    var selectedEndDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedEndTime by remember { mutableStateOf(LocalTime.of(7, 0)) }
    var selectedSleepStage by remember { mutableStateOf(SleepSessionRecord.STAGE_TYPE_SLEEPING) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showSleepStageDropdown by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()) }

    val sleepStages = listOf(
        "Light Sleep" to SleepSessionRecord.STAGE_TYPE_LIGHT,
        "Deep Sleep" to SleepSessionRecord.STAGE_TYPE_DEEP,
        "REM Sleep" to SleepSessionRecord.STAGE_TYPE_REM,
        "Awake" to SleepSessionRecord.STAGE_TYPE_AWAKE,
        "Sleeping (Unspecified)" to SleepSessionRecord.STAGE_TYPE_SLEEPING,
        "Out of Bed" to SleepSessionRecord.STAGE_TYPE_OUT_OF_BED
    )


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sleep Session") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text("Sleep Stage", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(
                    expanded = showSleepStageDropdown,
                    onExpandedChange = { showSleepStageDropdown = it }
                ) {
                    OutlinedTextField(
                        value = sleepStages.first { it.second == selectedSleepStage }.first,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSleepStageDropdown)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = showSleepStageDropdown,
                        onDismissRequest = { showSleepStageDropdown = false }
                    ) {
                        sleepStages.forEach { (name, stage) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedSleepStage = stage
                                    showSleepStageDropdown = false
                                }
                            )
                        }
                    }
                }


                Text("Sleep Start", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { showStartDatePicker = true }) {
                        Text(selectedStartDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    }
                    OutlinedButton(onClick = { showStartTimePicker = true }) {
                        Text(selectedStartTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }

                Text("Sleep End", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = { showEndDatePicker = true }) {
                        Text(selectedEndDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    }
                    OutlinedButton(onClick = { showEndTimePicker = true }) {
                        Text(selectedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startDateTime = LocalDateTime.of(selectedStartDate, selectedStartTime)
                    val endDateTime = LocalDateTime.of(selectedEndDate, selectedEndTime)
                    val currentDateTime = LocalDateTime.now()

                    when {
                        startDateTime.isAfter(currentDateTime) -> {
                            errorMessage = "Start time cannot be in the future"
                        }

                        endDateTime.isAfter(currentDateTime) -> {
                            errorMessage = "End time cannot be in the future"
                        }

                        endDateTime.isBefore(startDateTime) -> {
                            errorMessage = "End time must be after start time"
                        }

                        else -> {
                            onConfirm(startDateTime, endDateTime, selectedSleepStage)
                            onDismiss()
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(state.hour, state.minute)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = state)
        }
    )
}

