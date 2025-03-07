package com.demo.healthtracker

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {
    @Provides
    @Singleton
    fun provideHealthMonitor(
        healthManager: HealthManager,
        @ApplicationContext context: Context
    ): HealthDataMonitorV3 {
        return HealthDataMonitorV3(healthManager, context)
    }
}