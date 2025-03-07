package com.demo.healthtracker.workmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.demo.healthtracker.R
import com.demo.healthtracker.service2.HealthDataMonitorV2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HealthBackgroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "health_tracker_channel"

        // Helper method to start the service
        fun startService(context: Context) {
            val intent = Intent(context, HealthBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        // Helper method to stop the service
        fun stopService(context: Context) {
            val intent = Intent(context, HealthBackgroundService::class.java)
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var healthDataMonitorV2: HealthDataMonitorV2

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            healthDataMonitorV2.startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        healthDataMonitorV2.stopMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Health Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for health tracking service"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Tracker Active")
            .setContentText("Monitoring your health data in the background")
            .setSmallIcon(R.drawable.ic_launcher_background) // Make sure you have this icon in your project
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}