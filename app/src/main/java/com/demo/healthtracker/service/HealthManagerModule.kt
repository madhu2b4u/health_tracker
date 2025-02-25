package com.demo.healthtracker.service

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.demo.healthtracker.HealthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthManagerModule {

    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }

    @Provides
    @Singleton
    fun provideHealthManager(
        @ApplicationContext context: Context,
        healthConnectClient: HealthConnectClient
    ): HealthManager {
        return HealthManager(context, healthConnectClient)
    }

    @Provides
    @Singleton
    fun provideHealthDataMonitor(
        @ApplicationContext context: Context,
        healthManager: HealthManager
    ): HealthDataMonitor {
        return HealthDataMonitor(healthManager, context)
    }
}