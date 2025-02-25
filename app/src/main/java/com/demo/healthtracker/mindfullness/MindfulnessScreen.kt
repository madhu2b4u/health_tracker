package com.demo.healthtracker.mindfullness

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.formatDateTime
import com.demo.healthtracker.sleep.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


/*
    The warning “Restricted API” appears because MindfulnessSessionRecord is still marked as an experimental API in Health Connect.

🔍 Why is this happening?
	•	Google sometimes marks certain APIs as restricted when they are in early development or internal testing.
	•	Health Connect’s MindfulnessSessionRecord might not be fully stable yet.
 */

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen(
    viewModel: MindfulnessViewModel = hiltViewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val mindfulnessData by viewModel.mindfulnessData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Mindfulness Session")
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    MindfulnessCard(record, viewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        AddMindfulnessDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { startTime, endTime, title, notes, mindfulnessType ->
                viewModel.addMindfulnessSession(startTime, endTime, title, notes, mindfulnessType)
                showAddDialog = false
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("RestrictedApi")
@Composable
fun MindfulnessCard(
    @SuppressLint("RestrictedApi") record: MindfulnessSessionRecord,
    viewModel: MindfulnessViewModel
) {
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
                    text = viewModel.getMindfulnessTypeName(record.mindfulnessSessionType),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Duration: ${viewModel.getDuration(record.startTime, record.endTime)}",
                style = MaterialTheme.typography.bodyMedium
            )

            record.notes?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Started: ${formatDateTime(record.startTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ended: ${formatDateTime(record.endTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMindfulnessDialog(
    onDismiss: () -> Unit,
    onAdd: (LocalDateTime, LocalDateTime, String, String?, Int) -> Unit
) {
    var selectedStartDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedStartTime by remember { mutableStateOf(LocalTime.now().minusMinutes(30)) }
    var selectedEndDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedEndTime by remember { mutableStateOf(LocalTime.now()) }
    var title by remember { mutableStateOf("Mindfulness Session") }
    var notes by remember { mutableStateOf("") }
    var selectedMindfulnessType by remember {
        mutableStateOf(MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION)
    }
    var showTypeMenu by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val mindfulnessTypes = listOf(
        "Meditation" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION,
        "Breathing" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING,
        "Movement" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT,
        "Music" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC,
        "Unguided" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED,
        "Other" to MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Mindfulness Session") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = showTypeMenu,
                    onExpandedChange = { showTypeMenu = it }
                ) {
                    OutlinedTextField(
                        value = mindfulnessTypes.find { it.second == selectedMindfulnessType }?.first
                            ?: "Meditation",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Session Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeMenu) },
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        mindfulnessTypes.forEach { (name, type) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedMindfulnessType = type
                                    showTypeMenu = false
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
                    if (title.isNotEmpty() && endDateTime.isAfter(startDateTime)) {
                        onAdd(
                            startDateTime,
                            endDateTime,
                            title,
                            if (notes.isEmpty()) null else notes,
                            selectedMindfulnessType
                        )
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