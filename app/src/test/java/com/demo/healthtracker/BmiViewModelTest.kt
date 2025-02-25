package com.demo.healthtracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.units.Mass
import com.demo.healthtracker.bmi.BMIViewModel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.rules.TestRule
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExperimentalCoroutinesApi
class BmiViewModelTest {

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockHealthManager: HealthManager
    private lateinit var viewModel: BMIViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockHealthManager = mockk()
        viewModel = BMIViewModel(mockHealthManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBmiData should update state with data from health manager`() = runTest {
        // Given
        val mockRecord1 = createMockBmiRecord(70.0, Instant.now().minus(1, ChronoUnit.HOURS))
        val mockRecord2 = createMockBmiRecord(71.0, Instant.now().minus(2, ChronoUnit.HOURS))
        val mockRecords = listOf(mockRecord1, mockRecord2)
        
        coEvery { 
            mockHealthManager.readBmiData(any(), any()) 
        } returns mockRecords

        // When
        viewModel.loadBmiData()

        // Then
        val data = viewModel.bmiData.first()
        assertEquals(2, data.size)
        assertEquals(mockRecord1, data[0])
        assertEquals(mockRecord2, data[1])
    }

    @Test
    fun `loadBmiData should handle empty data`() = runTest {
        // Given
        coEvery { 
            mockHealthManager.readBmiData(any(), any()) 
        } returns emptyList()

        // When
        viewModel.loadBmiData()

        // Then
        val data = viewModel.bmiData.first()
        assertTrue(data.isEmpty())
    }

    @Test
    fun `addBmi should call health manager and reload data`() = runTest {
        // Given
        val height = 1.75f
        val weight = 70.0f
        
        coEvery { 
            mockHealthManager.writeBmiData(height, weight) 
        } returns Unit
        
        val mockRecord = createMockBmiRecord(weight.toDouble(), Instant.now())
        coEvery { 
            mockHealthManager.readBmiData(any(), any()) 
        } returns listOf(mockRecord)

        // When
        viewModel.addBmi(height, weight)

        // Then
        coVerify { mockHealthManager.writeBmiData(height, weight) }
        coVerify { mockHealthManager.readBmiData(any(), any()) }
        
        val data = viewModel.bmiData.first()
        assertEquals(1, data.size)
        assertEquals(mockRecord, data[0])
    }

    @Test
    fun `getBmiCategory should return Underweight for BMI less than 18_5`() {
        // Create mock Mass objects
        val mockMass1 = mockk<Mass>()
        val mockMass2 = mockk<Mass>()
        val mockMass3 = mockk<Mass>()

        // Set up their inKilograms property
        every { mockMass1.inKilograms } returns 18.4
        every { mockMass2.inKilograms } returns 17.0
        every { mockMass3.inKilograms } returns 16.0

        // Test with the mock Mass objects
        assertEquals("Underweight", viewModel.getBmiCategory(mockMass1))
        assertEquals("Underweight", viewModel.getBmiCategory(mockMass2))
        assertEquals("Underweight", viewModel.getBmiCategory(mockMass3))
    }

    @Test
    fun `getBmiCategory should return Normal for BMI between 18_5 and 24_9`() {
        val mockMass1 = mockk<Mass>()
        val mockMass2 = mockk<Mass>()
        val mockMass3 = mockk<Mass>()

        every { mockMass1.inKilograms } returns 18.5
        every { mockMass2.inKilograms } returns 22.0
        every { mockMass3.inKilograms } returns 24.9

        assertEquals("Normal", viewModel.getBmiCategory(mockMass1))
        assertEquals("Normal", viewModel.getBmiCategory(mockMass2))
        assertEquals("Normal", viewModel.getBmiCategory(mockMass3))
    }

    @Test
    fun `getBmiCategory should return Overweight for BMI between 25 and 29_9`() {
        val mockMass1 = mockk<Mass>()
        val mockMass2 = mockk<Mass>()
        val mockMass3 = mockk<Mass>()

        every { mockMass1.inKilograms } returns 25.0
        every { mockMass2.inKilograms } returns 27.5
        every { mockMass3.inKilograms } returns 29.9

        assertEquals("Overweight", viewModel.getBmiCategory(mockMass1))
        assertEquals("Overweight", viewModel.getBmiCategory(mockMass2))
        assertEquals("Overweight", viewModel.getBmiCategory(mockMass3))
    }

    @Test
    fun `getBmiCategory should return Obese for BMI 30 and above`() {
        val mockMass1 = mockk<Mass>()
        val mockMass2 = mockk<Mass>()
        val mockMass3 = mockk<Mass>()

        every { mockMass1.inKilograms } returns 30.0
        every { mockMass2.inKilograms } returns 35.0
        every { mockMass3.inKilograms } returns 40.0

        assertEquals("Obese", viewModel.getBmiCategory(mockMass1))
        assertEquals("Obese", viewModel.getBmiCategory(mockMass2))
        assertEquals("Obese", viewModel.getBmiCategory(mockMass3))
    }

    @Test
    fun `collectFlowData emits all updates`() = runTest {
        // Given
        val record1 = createMockBmiRecord(70.0, Instant.now().minus(3, ChronoUnit.HOURS))
        val record2 = createMockBmiRecord(71.0, Instant.now().minus(2, ChronoUnit.HOURS))
        val record3 = createMockBmiRecord(72.0, Instant.now().minus(1, ChronoUnit.HOURS))
        
        val allEmissions = mutableListOf<List<LeanBodyMassRecord>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.bmiData.toList(allEmissions)
        }

        // Initial state
        coEvery { 
            mockHealthManager.readBmiData(any(), any()) 
        } returns listOf(record1)
        viewModel.loadBmiData()

        // Update 1
        coEvery { 
            mockHealthManager.readBmiData(any(), any()) 
        } returns listOf(record1, record2)
        viewModel.loadBmiData()

        // Update 2
        coEvery { 
            mockHealthManager.readBmiData(any(), any()) 
        } returns listOf(record1, record2, record3)
        viewModel.loadBmiData()

        // Then
        collectJob.cancel()
        
        // Should have 4 emissions: initial empty list + 3 updates
        assertEquals(4, allEmissions.size)
        assertEquals(0, allEmissions[0].size)
        assertEquals(1, allEmissions[1].size)
        assertEquals(2, allEmissions[2].size)
        assertEquals(3, allEmissions[3].size)
    }

    private fun createMockBmiRecord(
        weight: Double,
        time: Instant
    ): LeanBodyMassRecord {
        val mockMass = mockk<Mass>()
        every { mockMass.inKilograms } returns weight
        
        val mockRecord = mockk<LeanBodyMassRecord>()
        every { mockRecord.mass } returns mockMass
        every { mockRecord.time } returns time
        
        return mockRecord
    }
}