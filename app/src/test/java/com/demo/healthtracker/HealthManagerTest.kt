package com.demo.healthtracker

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HealthManagerTest {

    private lateinit var healthManager: HealthManager
    private lateinit var mockContext: Context
    private lateinit var mockHealthConnectClient: HealthConnectClient
    private val testStartTime = Instant.now().minus(7, ChronoUnit.DAYS)
    private val testEndTime = Instant.now()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockContext = mockk()
        mockHealthConnectClient = mockk()
        healthManager = HealthManager(mockContext, mockHealthConnectClient)
    }

    @Test
    fun `readHeartRateData should return heart rate records`() = runBlocking {
        // Given
        val mockRecord = mockk<HeartRateRecord>()
        val mockResponse = mockk<ReadRecordsResponse<HeartRateRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        
        val requestSlot = slot<ReadRecordsRequest<HeartRateRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readHeartRateData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(HeartRateRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeHeartRateData should insert heart rate record`() = runBlocking {
        // Given
        val bpm = 75L
        val recordSlot = slot<List<HeartRateRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeHeartRateData(bpm)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as HeartRateRecord
        assertEquals(1, record.samples.size)
        assertEquals(bpm, record.samples[0].beatsPerMinute)
    }

    @Test
    fun `readStepsData should return steps records`() = runBlocking {
        // Given
        val mockRecord = mockk<StepsRecord>()
        val mockResponse = mockk<ReadRecordsResponse<StepsRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        
        val requestSlot = slot<ReadRecordsRequest<StepsRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readStepsData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(StepsRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeStepsData should insert steps record`() = runBlocking {
        // Given
        val steps = 10000L
        val recordSlot = slot<List<StepsRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeStepsData(steps)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as StepsRecord
        assertEquals(steps, record.count)
    }

    @Test
    fun `readSleepData should return sleep records`() = runBlocking {
        // Given
        val mockRecord = mockk<SleepSessionRecord>()
        val mockResponse = mockk<ReadRecordsResponse<SleepSessionRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        
        val requestSlot = slot<ReadRecordsRequest<SleepSessionRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readSleepData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(SleepSessionRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeSleepData should insert sleep record`() = runBlocking {
        // Given
        val startTime = Instant.now().minus(8, ChronoUnit.HOURS)
        val endTime = Instant.now()
        val stage = SleepSessionRecord.STAGE_TYPE_SLEEPING
        val recordSlot = slot<List<SleepSessionRecord>>()

        // Instead of mocking ZoneId, use a real ZoneOffset
        val zoneOffset = ZoneOffset.UTC

        coEvery {
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeSleepData(startTime, endTime, stage)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as SleepSessionRecord
        assertEquals(startTime, record.startTime)
        assertEquals(endTime, record.endTime)
        assertEquals(1, record.stages.size)
        assertEquals(stage, record.stages[0].stage)
    }

    @Test
    fun `readBmiData should return lean body mass records`() = runBlocking {
        // Given
        val mockRecord = mockk<LeanBodyMassRecord>()
        val mockResponse = mockk<ReadRecordsResponse<LeanBodyMassRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.time } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<LeanBodyMassRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readBmiData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(LeanBodyMassRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeBmiData should insert lean body mass record`() = runBlocking {
        // Given
        val height = 1.75f
        val weight = 70.0f
        val recordSlot = slot<List<LeanBodyMassRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeBmiData(height, weight)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as LeanBodyMassRecord
        assertEquals(weight.toDouble(), record.mass.inKilograms, 0.01)
    }

    @Test
    fun `readBloodOxygenData should return oxygen saturation records`() = runBlocking {
        // Given
        val mockRecord = mockk<OxygenSaturationRecord>()
        val mockResponse = mockk<ReadRecordsResponse<OxygenSaturationRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.time } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<OxygenSaturationRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readBloodOxygenData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(OxygenSaturationRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeBloodOxygenData should insert oxygen saturation record`() = runBlocking {
        // Given
        val percentage = 98.0
        val recordSlot = slot<List<OxygenSaturationRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeBloodOxygenData(percentage)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as OxygenSaturationRecord
        assertEquals(percentage, record.percentage.value, 0.01)
    }

    @Test
    fun `readBloodPressureData should return blood pressure records`() = runBlocking {
        // Given
        val mockRecord = mockk<BloodPressureRecord>()
        val mockResponse = mockk<ReadRecordsResponse<BloodPressureRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.time } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<BloodPressureRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readBloodPressureData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(BloodPressureRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeBloodPressureData should insert blood pressure record`() = runBlocking {
        // Given
        val systolic = 120.0
        val diastolic = 80.0
        val recordSlot = slot<List<BloodPressureRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeBloodPressureData(systolic, diastolic)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as BloodPressureRecord
        assertEquals(systolic, record.systolic.inMillimetersOfMercury, 0.01)
        assertEquals(diastolic, record.diastolic.inMillimetersOfMercury, 0.01)
    }

    @Test
    fun `readRespiratoryData should return respiratory rate records`() = runBlocking {
        // Given
        val mockRecord = mockk<RespiratoryRateRecord>()
        val mockResponse = mockk<ReadRecordsResponse<RespiratoryRateRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.time } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<RespiratoryRateRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readRespiratoryData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(RespiratoryRateRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeRespiratoryData should insert respiratory rate record`() = runBlocking {
        // Given
        val rate = 15.0
        val recordSlot = slot<List<RespiratoryRateRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeRespiratoryData(rate)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as RespiratoryRateRecord
        assertEquals(rate, record.rate, 0.01)
    }

    @Test
    fun `readWorkoutData should return exercise session records`() = runBlocking {
        // Given
        val mockRecord = mockk<ExerciseSessionRecord>()
        val mockResponse = mockk<ReadRecordsResponse<ExerciseSessionRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.startTime } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<ExerciseSessionRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readWorkoutData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(ExerciseSessionRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeWorkoutData should insert exercise session record`() = runBlocking {
        // Given
        val startTime = Instant.now().minus(1, ChronoUnit.HOURS)
        val endTime = Instant.now()
        val exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        val title = "Morning Run"
        val recordSlot = slot<List<ExerciseSessionRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeWorkoutData(startTime, endTime, exerciseType, title)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as ExerciseSessionRecord
        assertEquals(startTime, record.startTime)
        assertEquals(endTime, record.endTime)
        assertEquals(exerciseType, record.exerciseType)
        assertEquals(title, record.title)
    }

    @Test
    fun `readDistanceData should return distance records`() = runBlocking {
        // Given
        val mockRecord = mockk<DistanceRecord>()
        val mockResponse = mockk<ReadRecordsResponse<DistanceRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.startTime } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<DistanceRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readDistanceData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(DistanceRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeDistanceData should insert distance record`() = runBlocking {
        // Given
        val distance = 5.0 // 5 km
        val recordSlot = slot<List<DistanceRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeDistanceData(distance)

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as DistanceRecord
        assertEquals(distance, record.distance.inKilometers, 0.01)
    }

    @Test
    fun `readMindfulnessData should return mindfulness session records`() = runBlocking {
        // Given
        val mockRecord = mockk<MindfulnessSessionRecord>()
        val mockResponse = mockk<ReadRecordsResponse<MindfulnessSessionRecord>>()
        every { mockResponse.records } returns listOf(mockRecord)
        every { mockRecord.startTime } returns Instant.now()
        
        val requestSlot = slot<ReadRecordsRequest<MindfulnessSessionRecord>>()
        coEvery { 
            mockHealthConnectClient.readRecords(capture(requestSlot)) 
        } returns mockResponse

        // When
        val result = healthManager.readMindfulnessData(testStartTime, testEndTime)

        // Then
        assertEquals(1, result.size)
        assertEquals(mockRecord, result[0])
        assertEquals(MindfulnessSessionRecord::class, requestSlot.captured.recordType)
    }

    @Test
    fun `writeMindfulnessData should insert mindfulness session record`() = runBlocking {
        // Given
        val title = "Morning Meditation"
        val notes = "Felt very relaxed"
        val mindfulnessType = MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
        val recordSlot = slot<List<MindfulnessSessionRecord>>()
        
        coEvery { 
            mockHealthConnectClient.insertRecords(capture(recordSlot))
        } returns mockk()

        // When
        healthManager.writeMindfulnessData(
            title = title,
            notes = notes,
            mindfulnessType = mindfulnessType
        )

        // Then
        coVerify { mockHealthConnectClient.insertRecords(any()) }
        assertEquals(1, recordSlot.captured.size)
        val record = recordSlot.captured[0] as MindfulnessSessionRecord
        assertEquals(title, record.title)
        assertEquals(notes, record.notes)
        assertEquals(mindfulnessType, record.mindfulnessSessionType)
    }

    @Test
    fun `readHeartRateData should handle exceptions`() = runBlocking {
        // Given
        coEvery {
            mockHealthConnectClient.readRecords<HeartRateRecord>(any())
        } throws Exception("Test exception")

        // When
        val result = healthManager.readHeartRateData(testStartTime, testEndTime)

        // Then
        assertEquals(0, result.size)
    }

}