package com.demo.healthtracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.units.Pressure
import com.demo.healthtracker.bloodpressure.BloodPressureViewModel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.rules.TestRule
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
class BloodPressureViewModelTest {

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockHealthManager: HealthManager
    private lateinit var viewModel: BloodPressureViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockHealthManager = mockk()
        viewModel = BloodPressureViewModel(mockHealthManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBloodPressureData should update state with data from health manager`() = runTest {
        // Given
        val mockRecord1 = createMockBloodPressureRecord(120.0, 80.0, Instant.now().minus(1, ChronoUnit.HOURS))
        val mockRecord2 = createMockBloodPressureRecord(115.0, 75.0, Instant.now().minus(2, ChronoUnit.HOURS))
        val mockRecords = listOf(mockRecord1, mockRecord2)

        // Use relaxed matching for parameters
        coEvery {
            mockHealthManager.readBloodPressureData(any(), any())
        } returns mockRecords

        // When
        viewModel.loadBloodPressureData()

        // Then
        val data = viewModel.bloodPressureData.first()
        assertEquals(2, data.size)
        assertEquals(mockRecord1, data[0])
        assertEquals(mockRecord2, data[1])
    }

    @Test
    fun `loadBloodPressureData should handle empty data`() = runTest {
        // Given
        coEvery { 
            mockHealthManager.readBloodPressureData(any(), any()) 
        } returns emptyList()

        // When
        viewModel.loadBloodPressureData()

        // Then
        val data = viewModel.bloodPressureData.first()
        assertTrue(data.isEmpty())
    }

    @Test
    fun `addBloodPressure should call health manager and reload data`() = runTest {
        // Given
        val systolic = 120.0
        val diastolic = 80.0
        
        coEvery { 
            mockHealthManager.writeBloodPressureData(systolic, diastolic) 
        } returns Unit
        
        val mockRecord = createMockBloodPressureRecord(systolic, diastolic, Instant.now())
        coEvery { 
            mockHealthManager.readBloodPressureData(any(), any()) 
        } returns listOf(mockRecord)

        // When
        viewModel.addBloodPressure(systolic, diastolic)

        // Then
        coVerify { mockHealthManager.writeBloodPressureData(systolic, diastolic) }
        coVerify { mockHealthManager.readBloodPressureData(any(), any()) }
        
        val data = viewModel.bloodPressureData.first()
        assertEquals(1, data.size)
        assertEquals(mockRecord, data[0])
    }

    @Test
    fun `getBloodPressureCategory should return correct category for normal pressure`() {
        assertEquals("Normal", viewModel.getBloodPressureCategory(115.0, 75.0))
        assertEquals("Normal", viewModel.getBloodPressureCategory(119.0, 79.0))
    }
    
    @Test
    fun `getBloodPressureCategory should return correct category for elevated pressure`() {
        assertEquals("Elevated", viewModel.getBloodPressureCategory(120.0, 75.0))
        assertEquals("Elevated", viewModel.getBloodPressureCategory(129.0, 79.0))
    }
    
    @Test
    fun `getBloodPressureCategory should return correct category for stage 1 hypertension`() {
        assertEquals("Stage 1", viewModel.getBloodPressureCategory(130.0, 80.0))
        assertEquals("Stage 1", viewModel.getBloodPressureCategory(139.0, 89.0))
    }
    
    @Test
    fun `getBloodPressureCategory should return correct category for stage 2 hypertension`() {
        assertEquals("Stage 2", viewModel.getBloodPressureCategory(140.0, 90.0))
        assertEquals("Stage 2", viewModel.getBloodPressureCategory(180.0, 120.0))
    }

    @Test
    fun `collectFlowData emits all updates`() = runTest {
        // Given
        val record1 = createMockBloodPressureRecord(110.0, 70.0, Instant.now().minus(3, ChronoUnit.HOURS))
        val record2 = createMockBloodPressureRecord(120.0, 80.0, Instant.now().minus(2, ChronoUnit.HOURS))
        val record3 = createMockBloodPressureRecord(130.0, 85.0, Instant.now().minus(1, ChronoUnit.HOURS))
        
        val allEmissions = mutableListOf<List<BloodPressureRecord>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.bloodPressureData.toList(allEmissions)
        }

        // Initial state
        coEvery { 
            mockHealthManager.readBloodPressureData(any(), any()) 
        } returns listOf(record1)
        viewModel.loadBloodPressureData()

        // Update 1
        coEvery { 
            mockHealthManager.readBloodPressureData(any(), any()) 
        } returns listOf(record1, record2)
        viewModel.loadBloodPressureData()

        // Update 2
        coEvery { 
            mockHealthManager.readBloodPressureData(any(), any()) 
        } returns listOf(record1, record2, record3)
        viewModel.loadBloodPressureData()

        // Then
        collectJob.cancel()
        
        // Should have 4 emissions: initial empty list + 3 updates
        assertEquals(4, allEmissions.size)
        assertEquals(0, allEmissions[0].size)
        assertEquals(1, allEmissions[1].size)
        assertEquals(2, allEmissions[2].size)
        assertEquals(3, allEmissions[3].size)
    }

    private fun createMockBloodPressureRecord(
        systolic: Double, 
        diastolic: Double, 
        time: Instant
    ): BloodPressureRecord {
        val mockSystolic = mockk<Pressure>()
        val mockDiastolic = mockk<Pressure>()
        
        every { mockSystolic.inMillimetersOfMercury } returns systolic
        every { mockDiastolic.inMillimetersOfMercury } returns diastolic
        
        val mockRecord = mockk<BloodPressureRecord>()
        every { mockRecord.systolic } returns mockSystolic
        every { mockRecord.diastolic } returns mockDiastolic
        every { mockRecord.time } returns time
        
        return mockRecord
    }
}