package com.demo.healthtracker.workmanager

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    
    @Provides
    @Singleton
    fun provideHealthDataWorkerManager(
        @ApplicationContext context: Context
    ): HealthDataWorkerManager {
        return HealthDataWorkerManager(context)
    }
}