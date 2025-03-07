package com.demo.healthtracker.workmanager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.healthtracker.service.HealthDataV2

@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel()
) {
    // Collect the health data from the ViewModel
    val healthData by viewModel.healthData.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Health Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.refreshHealthData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Health Data")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (healthData != null) {
                HealthDataContent(healthData = healthData!!)
            } else {
                Text("Loading health data...")
            }
        }
    }
}

@Composable
fun HealthDataContent(healthData: HealthDataV2) {
    Column {
        // Steps Card
        healthData.steps.firstOrNull()?.let { stepsRecord ->
            HealthCard(
                title = "Steps",
                value = "${stepsRecord.count} steps",
                time = "Last updated: ${stepsRecord.startTime}"
            )
        }
        
        // Heart Rate Card
        healthData.heartRate.firstOrNull()?.let { heartRateRecord ->
            val bpm = heartRateRecord.samples.firstOrNull()?.beatsPerMinute ?: 0
            HealthCard(
                title = "Heart Rate",
                value = "$bpm BPM",
                time = "Last updated: ${heartRateRecord.startTime}"
            )
        }
        
        // Blood Pressure Card
        healthData.bloodPressure.firstOrNull()?.let { bpRecord ->
            val systolic = bpRecord.systolic.inMillimetersOfMercury.toInt()
            val diastolic = bpRecord.diastolic.inMillimetersOfMercury.toInt()
            HealthCard(
                title = "Blood Pressure",
                value = "$systolic/$diastolic mmHg",
                time = "Last updated: ${bpRecord.time}"
            )
        }
        
        // More health cards can be added here for other health metrics
    }
}

@Composable
fun HealthCard(title: String, value: String, time: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}