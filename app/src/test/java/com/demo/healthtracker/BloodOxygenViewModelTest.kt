package com.demo.healthtracker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.coVerify
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
import androidx.health.connect.client.records.OxygenSaturationRecord
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.rules.TestRule
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import androidx.health.connect.client.units.Percentage
import com.demo.healthtracker.bloodoxygen.BloodOxygenViewModel
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.spyk

@ExperimentalCoroutinesApi
class BloodOxygenViewModelTest {

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockHealthManager: HealthManager
    private lateinit var viewModel: BloodOxygenViewModel

    private val testStartTime = Instant.now().minus(7, ChronoUnit.DAYS)
    private val testEndTime = Instant.now()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockHealthManager = mockk()
        viewModel = BloodOxygenViewModel(mockHealthManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBloodOxygenData should update state with data from health manager`() = runTest {
        // Given
        val mockRecord1 = createMockOxygenRecord(98.0, Instant.now().minus(1, ChronoUnit.HOURS))
        val mockRecord2 = createMockOxygenRecord(97.0, Instant.now().minus(2, ChronoUnit.HOURS))
        val mockRecords = listOf(mockRecord1, mockRecord2)
        
        coEvery { 
            mockHealthManager.readBloodOxygenData(any(), any()) 
        } returns mockRecords

        // When
        viewModel.loadBloodOxygenData()

        // Then
        val data = viewModel.bloodOxygenData.first()
        assertEquals(2, data.size)
        assertEquals(mockRecord1, data[0])
        assertEquals(mockRecord2, data[1])
    }

    @Test
    fun `loadBloodOxygenData should handle empty data`() = runTest {
        // Given
        coEvery { 
            mockHealthManager.readBloodOxygenData(any(), any()) 
        } returns emptyList()

        // When
        viewModel.loadBloodOxygenData()

        // Then
        val data = viewModel.bloodOxygenData.first()
        assertTrue(data.isEmpty())
    }

    @Test
    fun `addBloodOxygen should call health manager and reload data`() = runTest {
        // Given
        val percentage = 98.0

        // Mock the write operation
        coEvery {
            mockHealthManager.writeBloodOxygenData(percentage)
        } returns Unit

        // Mock the read operation with ANY parameters to match what the ViewModel uses
        val mockRecord = createMockOxygenRecord(percentage, Instant.now())
        coEvery {
            mockHealthManager.readBloodOxygenData(any(), any())
        } returns listOf(mockRecord)

        // Spy on the loadBloodOxygenData method to verify it's called
        val spyViewModel = spyk(viewModel)
        coEvery { spyViewModel.loadBloodOxygenData() } just runs

        // When
        spyViewModel.addBloodOxygen(percentage)

        // Then
        coVerify { mockHealthManager.writeBloodOxygenData(percentage) }
        coVerify { spyViewModel.loadBloodOxygenData() }
    }

    @Test
    fun `getOxygenCategory should return correct category`() {
        // Normal
        assertEquals("Normal", viewModel.getOxygenCategory(98.0))
        assertEquals("Normal", viewModel.getOxygenCategory(95.0))
        
        // Mild Hypoxemia
        assertEquals("Mild Hypoxemia", viewModel.getOxygenCategory(94.0))
        assertEquals("Mild Hypoxemia", viewModel.getOxygenCategory(90.0))
        
        // Moderate Hypoxemia
        assertEquals("Moderate Hypoxemia", viewModel.getOxygenCategory(89.0))
        assertEquals("Moderate Hypoxemia", viewModel.getOxygenCategory(85.0))
        
        // Severe Hypoxemia
        assertEquals("Severe Hypoxemia", viewModel.getOxygenCategory(84.0))
        assertEquals("Severe Hypoxemia", viewModel.getOxygenCategory(70.0))
    }

    @Test
    fun `collectFlowData emits all updates`() = runTest {
        // Given
        val record1 = createMockOxygenRecord(96.0, Instant.now().minus(3, ChronoUnit.HOURS))
        val record2 = createMockOxygenRecord(97.0, Instant.now().minus(2, ChronoUnit.HOURS))
        val record3 = createMockOxygenRecord(98.0, Instant.now().minus(1, ChronoUnit.HOURS))
        
        val allEmissions = mutableListOf<List<OxygenSaturationRecord>>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.bloodOxygenData.toList(allEmissions)
        }

        // Initial state
        coEvery { 
            mockHealthManager.readBloodOxygenData(any(), any()) 
        } returns listOf(record1)
        viewModel.loadBloodOxygenData()

        // Update 1
        coEvery { 
            mockHealthManager.readBloodOxygenData(any(), any()) 
        } returns listOf(record1, record2)
        viewModel.loadBloodOxygenData()

        // Update 2
        coEvery { 
            mockHealthManager.readBloodOxygenData(any(), any()) 
        } returns listOf(record1, record2, record3)
        viewModel.loadBloodOxygenData()

        // Then
        collectJob.cancel()
        
        // Should have 4 emissions: initial empty list + 3 updates
        assertEquals(4, allEmissions.size)
        assertEquals(0, allEmissions[0].size)
        assertEquals(1, allEmissions[1].size)
        assertEquals(2, allEmissions[2].size)
        assertEquals(3, allEmissions[3].size)
    }

    private fun createMockOxygenRecord(value: Double, time: Instant): OxygenSaturationRecord {
        val mockPercentage = mockk<Percentage>()
        every { mockPercentage.value } returns value
        
        val mockRecord = mockk<OxygenSaturationRecord>()
        every { mockRecord.percentage } returns mockPercentage
        every { mockRecord.time } returns time
        
        return mockRecord
    }
}