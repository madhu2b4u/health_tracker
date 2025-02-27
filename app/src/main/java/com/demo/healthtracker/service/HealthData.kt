package com.demo.healthtracker.service

import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
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
    val sleep: List<SleepSessionRecord>,
)

data class HealthDataV2(
    val steps: List<StepsRecord>,
    val heartRate: List<HeartRateRecord>,
    val bloodPressure: List<BloodPressureRecord>,
    val bloodOxygen: List<OxygenSaturationRecord>,
    val respiratory: List<RespiratoryRateRecord>,
    val workout: List<ExerciseSessionRecord>,
    val sleep: List<SleepSessionRecord>,
    val bmi: List<LeanBodyMassRecord>,
    val distance: List<DistanceRecord>,
)