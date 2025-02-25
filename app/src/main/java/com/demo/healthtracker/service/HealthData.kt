package com.demo.healthtracker.service

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

data class HealthData(
    val steps: List<StepsRecord>,
    val heartRate: List<HeartRateRecord>,
    val bloodPressure: List<BloodPressureRecord>,
    val bloodOxygen: List<OxygenSaturationRecord>,
    val respiratory: List<RespiratoryRateRecord>,
    val workout: List<ExerciseSessionRecord>,
    val sleep: List<SleepSessionRecord>
)