package com.demo.healthtracker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthPermissionsScreen(
    onPermissionsGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    var permissionState by remember { mutableStateOf<PermissionState>(PermissionState.Checking) }

    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }

    // Health permissions to request
    val healthPermissions = remember {
        listOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),

            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),

            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),

            HealthPermission.getReadPermission(LeanBodyMassRecord::class),
            HealthPermission.getWritePermission(LeanBodyMassRecord::class),

            // Oxygen Saturation
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getWritePermission(OxygenSaturationRecord::class),

            // Blood Pressure
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getWritePermission(BloodPressureRecord::class),

            // Respiratory Rate
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getWritePermission(RespiratoryRateRecord::class),

            // Exercise Sessions
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),


            // Distance
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),

            // Mindfulness Sessions
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
            HealthPermission.getWritePermission(MindfulnessSessionRecord::class)

          /*

            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),



            // Height
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getWritePermission(HeightRecord::class),

            // Exercise Sessions
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),


           */
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        permissionState = if (granted.containsAll(healthPermissions)) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }

    LaunchedEffect(Unit) {
        try {
            val hasPermissions = healthConnectClient.permissionController.getGrantedPermissions()
            if (hasPermissions.containsAll(healthPermissions)) {
                permissionState = PermissionState.Granted
                onPermissionsGranted()
            } else {
                try {
                    healthConnectClient.permissionController.getGrantedPermissions()
                    permissionLauncher.launch(healthPermissions.toSet())
                    permissionState = PermissionState.Checking
                } catch (e: Exception) {
                    Log.e("HealthConnect", "Health Connect not available", e)
                    permissionState = PermissionState.HealthConnectRequired
                }
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking permissions", e)
            permissionState = PermissionState.HealthConnectRequired
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Health Permissions") }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (permissionState) {
                PermissionState.Checking -> {
                    CircularProgressIndicator()
                }

                PermissionState.HealthConnectRequired -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Health Connect Required",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "This app requires Health Connect. Please install it from the Play Store.",
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data =
                                        Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Install Health Connect")
                        }
                    }
                }

                PermissionState.Denied -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Permissions Required",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Health data permissions are required for the app to function properly.",
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                permissionLauncher.launch(healthPermissions.toTypedArray().toSet())
                            }
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }

                PermissionState.Granted -> {
                    Text("All permissions granted!")
                }
            }
        }
    }
}

sealed class PermissionState {
    object Checking : PermissionState()
    object HealthConnectRequired : PermissionState()
    object Denied : PermissionState()
    object Granted : PermissionState()
}

@Composable
private fun HealthConnectRequiredContent(onInstallClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Health Connect Required",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "This app requires Health Connect to function properly. Please install it.",
            textAlign = TextAlign.Center
        )
        Button(onClick = onInstallClick) {
            Text("Install Health Connect")
        }
    }
}

@Composable
private fun PermissionsDeniedContent(onRequestAgain: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Health data permissions are required for the app to function properly.",
            textAlign = TextAlign.Center
        )
        Button(onClick = onRequestAgain) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun PermissionSettingsDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = {
            Text("Health data permissions are required. Please enable them in Settings.")
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text("Go to Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions
private fun isHealthConnectAvailable(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

private fun hasRequiredPermissions(context: Context, permissions: List<String>): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
    return if (context is Activity) {
        ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
    } else {
        false
    }
}

private fun openHealthConnectPlayStore(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback for devices without Play Store
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            data =
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}