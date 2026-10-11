package com.yawmiyati.app

import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.data.jobs.JobRepository
import com.yawmiyati.app.data.jobs.JobResult
import com.yawmiyati.app.viewmodels.JobsUiState
import com.yawmiyati.app.viewmodels.JobsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pagination verification — ensures JobsViewModel accumulates 25 jobs
 * across two pages (PAGE_SIZE = 20) using a fake in-memory repository.
 *
 * JobsViewModel.init calls loadJobs(); tests advance past that call first,
 * then issue their own requests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Fake repository (25 jobs, page size 20) ────────────────────────────────

    private val totalJobs = (1..25).map { dummyJob(it.toString()) }

    private fun makeFakeRepo() = object : JobRepository() {
        override suspend fun listJobs(city: String?, page: Int): JobResult<PaginatedJobResult> {
            val pageSize = 20
            val from = (page - 1) * pageSize
            val to = minOf(from + pageSize, totalJobs.size)
            val slice = if (from >= totalJobs.size) emptyList() else totalJobs.subList(from, to)
            return JobResult.Success(PaginatedJobResult(slice, to < totalJobs.size, totalJobs.size))
        }
    }

    // ── Tests ──────────────────────────────────────────────────────────────────

    @Test
    fun `first page contains 20 jobs with hasMore true`() = runTest {
        val vm = JobsViewModel(makeFakeRepo())
        advanceUntilIdle()   // drain init's loadJobs()

        val state = vm.uiState.value
        assertTrue("expected Success", state is JobsUiState.Success)
        state as JobsUiState.Success
        assertEquals(20, state.jobs.size)
        assertTrue("hasMore should be true after page 1", state.hasMore)
        assertFalse(state.isRefreshing)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `loadNextPage accumulates 25 jobs across two pages`() = runTest {
        val vm = JobsViewModel(makeFakeRepo())
        advanceUntilIdle()   // page 1 loaded

        vm.loadNextPage()
        advanceUntilIdle()   // page 2 loaded

        val state = vm.uiState.value
        assertTrue("expected Success after page 2", state is JobsUiState.Success)
        state as JobsUiState.Success
        assertEquals(25, state.jobs.size)
        assertFalse("hasMore should be false after last page", state.hasMore)
    }

    @Test
    fun `refresh resets to first 20 jobs after both pages loaded`() = runTest {
        val vm = JobsViewModel(makeFakeRepo())
        advanceUntilIdle()
        vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(25, (vm.uiState.value as JobsUiState.Success).jobs.size)

        vm.refresh()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("expected Success after refresh", state is JobsUiState.Success)
        state as JobsUiState.Success
        assertEquals("refresh must reset to page 1 (20 jobs)", 20, state.jobs.size)
        assertTrue(state.hasMore)
    }

    @Test
    fun `extra loadNextPage when hasMore is false is a no-op`() = runTest {
        val vm = JobsViewModel(makeFakeRepo())
        advanceUntilIdle()
        vm.loadNextPage()
        advanceUntilIdle()
        // All 25 loaded, hasMore = false

        vm.loadNextPage()   // should be ignored
        advanceUntilIdle()

        assertEquals(25, (vm.uiState.value as JobsUiState.Success).jobs.size)
    }

    @Test
    fun `empty repository emits Empty state`() = runTest {
        val emptyRepo = object : JobRepository() {
            override suspend fun listJobs(city: String?, page: Int): JobResult<PaginatedJobResult> =
                JobResult.Success(PaginatedJobResult(emptyList(), false, 0))
        }
        val vm = JobsViewModel(emptyRepo)
        advanceUntilIdle()

        assertTrue("expected Empty state", vm.uiState.value is JobsUiState.Empty)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun dummyJob(id: String) = JobListingResponse(
        id = id, title = "وظيفة $id", description = "",
        businessName = "شركة", businessVerificationStatus = "APPROVED",
        locationCity = "غزة", locationAddress = "شارع الرئيسي",
        workDate = "2026-10-20", startTime = "08:00:00", endTime = "17:00:00",
        numWorkersNeeded = 1, requiredSkills = "",
        wageAmount = 15000L, wageCurrency = "ILS", wageBasis = "PER_DAY",
        wageDisplay = "150 ILS / يوم",
        paymentMethod = "CASH", paymentDeadlineDays = 3,
        attendanceRequirements = "", cancellationPolicy = "", additionalConditions = "",
        status = "PUBLISHED", isFunded = false, publishedAt = null, version = 1,
        createdAt = "2026-10-11T00:00:00Z"
    )
}
