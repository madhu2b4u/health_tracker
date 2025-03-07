package com.demo.healthtracker.workmanager

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.work.WorkManager
import com.demo.healthtracker.HealthManager
import com.demo.healthtracker.service.HealthDataV2
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDataMonitorForWorkManager @Inject constructor(
    private val healthManager: HealthManager,
    @ApplicationContext private val context: Context,
    private val healthDataWorkerManager: HealthDataWorkerManager
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    // Reference the health data from the repository
    val healthData: StateFlow<HealthDataV2?> = HealthDataRepository.healthData

    /**
     * Start health data monitoring using WorkManager
     */
    fun startMonitoring() {
        Log.i("HealthMonitor", "Starting health data monitoring...")
        
        // Schedule the worker to run every 5 minutes
        healthDataWorkerManager.startMonitoring()
        
        // Trigger an immediate refresh to get initial data
        refreshData()
        
        Log.i("HealthMonitor", "Health monitoring started successfully")
    }

    /**
     * Request a one-time execution of the health data worker
     */
    fun refreshData() {
        Log.d("HealthMonitor", "Triggering a one-time health data refresh")
        
        // Create a one-time work request to run immediately
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<HealthDataWorker>()
            .build()
        
        // Enqueue the one-time work
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Stop health data monitoring
     */
    fun stopMonitoring() {
        Log.i("HealthMonitor", "Stopping health monitoring...")
        healthDataWorkerManager.stopMonitoring()
        Log.i("HealthMonitor", "Health monitoring stopped successfully")
    }
}