package com.demo.healthtracker.workmanager

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HealthDataWorkerManager @Inject constructor(
    private val context: Context
) {
    /**
     * Start periodic health data monitoring every 5 minutes
     */
    fun startMonitoring() {
        Log.i("HealthWorkerManager", "Starting health data monitoring with WorkManager...")
        
        // Define constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        // Create the periodic work request - run every 5 minutes
        val workRequest = PeriodicWorkRequestBuilder<HealthDataWorker>(
            5, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        // Schedule the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HealthDataWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Replace existing if any
            workRequest
        )
        
        Log.i("HealthWorkerManager", "Health monitoring scheduled successfully")
    }
    
    /**
     * Stop health data monitoring
     */
    fun stopMonitoring() {
        Log.i("HealthWorkerManager", "Stopping health monitoring...")
        WorkManager.getInstance(context).cancelUniqueWork(HealthDataWorker.WORK_NAME)
        Log.i("HealthWorkerManager", "Health monitoring stopped successfully")
    }
}