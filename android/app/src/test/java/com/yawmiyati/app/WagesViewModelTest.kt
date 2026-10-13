package com.yawmiyati.app

import com.yawmiyati.app.data.api.DeclarePaymentRequest
import com.yawmiyati.app.data.api.PaymentDeclarationResponse
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.data.auth.SessionEventBus
import com.yawmiyati.app.data.wages.WageRepository
import com.yawmiyati.app.data.wages.WageResult
import com.yawmiyati.app.viewmodels.WageActionState
import com.yawmiyati.app.viewmodels.WagesUiState
import com.yawmiyati.app.viewmodels.WagesViewModel
import com.yawmiyati.app.viewmodels.WorkerWagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WagesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun fakeObligation(id: String, status: String) = WageObligationResponse(
        id = id, assignment = "assign-$id", workerPhone = "+970599000001",
        jobTitle = "Day Work", amount = 10000L, currency = "ILS",
        amountDisplay = "100 ILS", dueDate = "2026-10-15", status = status,
        createdAt = "2026-10-10T08:00:00Z", declarations = emptyList()
    )

    private fun fakeDeclaration(id: String, obligationId: String) = PaymentDeclarationResponse(
        id = id, wageObligation = obligationId, declaredAmount = 10000L,
        currency = "ILS", paymentDate = "2026-10-10", paymentMethod = "CASH",
        transactionReference = "", declaredByPhone = "+970599000002",
        declaredAt = "2026-10-10T09:00:00Z", status = "SUBMITTED", reviewNotes = ""
    )

    // ── WagesViewModel (business) ─────────────────────────────────────────────

    @Test
    fun `loads obligations on init`() = runTest(testDispatcher) {
        val obligations = listOf(fakeObligation("1", "PENDING"), fakeObligation("2", "DUE"))
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(obligations)
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is WagesUiState.Success)
        assertEquals(2, (state as WagesUiState.Success).obligations.size)
    }

    @Test
    fun `empty obligations list emits Empty state`() = runTest(testDispatcher) {
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(emptyList<WageObligationResponse>())
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is WagesUiState.Empty)
    }

    @Test
    fun `successful declarePayment updates obligation status`() = runTest(testDispatcher) {
        val obligation = fakeObligation("obl-1", "PENDING")
        val declaration = fakeDeclaration("decl-1", "obl-1")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun declarePayment(id: String, request: DeclarePaymentRequest) =
                WageResult.Success(declaration)
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        vm.declarePayment("obl-1", 10000L, "2026-10-10", "CASH", "")
        advanceUntilIdle()
        assertEquals(WageActionState.Success, vm.actionState.value)
        val state = vm.uiState.value as WagesUiState.Success
        assertEquals("AWAITING_ACK", state.obligations.first().status)
    }

    @Test
    fun `declarePayment API error surfaces in actionState`() = runTest(testDispatcher) {
        val obligation = fakeObligation("obl-2", "PENDING")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun declarePayment(id: String, request: DeclarePaymentRequest) =
                WageResult.Error("خطأ في الشبكة")
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        vm.declarePayment("obl-2", 10000L, "2026-10-10", "CASH", "")
        advanceUntilIdle()
        val action = vm.actionState.value
        assertTrue(action is WageActionState.Error)
        assertEquals("خطأ في الشبكة", (action as WageActionState.Error).message)
    }

    // ── WorkerWagesViewModel ──────────────────────────────────────────────────

    @Test
    fun `worker acknowledges payment and obligation updates`() = runTest(testDispatcher) {
        val obligation = fakeObligation("w-obl-1", "AWAITING_ACK")
        val acknowledged = obligation.copy(status = "VERIFIED_PAID")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun acknowledgePayment(id: String) = WageResult.Success(acknowledged)
        }
        val vm = WorkerWagesViewModel(repo)
        advanceUntilIdle()
        vm.acknowledgePayment("w-obl-1")
        advanceUntilIdle()
        assertEquals(WageActionState.Success, vm.actionState.value)
        val state = vm.uiState.value as WagesUiState.Success
        assertEquals("VERIFIED_PAID", state.obligations.first().status)
    }

    @Test
    fun `worker disputePayment transitions obligation to DISPUTED status`() = runTest(testDispatcher) {
        val obligation = fakeObligation("w-obl-2", "AWAITING_ACK")
        val disputed = obligation.copy(status = "DISPUTED")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun disputePayment(id: String, description: String) = WageResult.Success(disputed)
        }
        val vm = WorkerWagesViewModel(repo)
        advanceUntilIdle()
        vm.disputePayment("w-obl-2", "لم أستلم أجري حتى الآن")
        advanceUntilIdle()
        assertEquals(WageActionState.Success, vm.actionState.value)
        val state = vm.uiState.value as WagesUiState.Success
        assertEquals("DISPUTED", state.obligations.first().status)
    }

    @Test
    fun `worker disputePayment auth error emits session expired and resets actionState`() = runTest(testDispatcher) {
        val obligation = fakeObligation("w-obl-3", "AWAITING_ACK")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun disputePayment(id: String, description: String) =
                WageResult.Error("غير مصرح", isAuth = true)
        }
        val events = mutableListOf<Unit>()
        val job = launch { SessionEventBus.sessionExpired.collect { events.add(it) } }
        val vm = WorkerWagesViewModel(repo)
        advanceUntilIdle()
        vm.disputePayment("w-obl-3", "لم أستلم")
        advanceUntilIdle()
        assertTrue("Expected session expired event", events.isNotEmpty())
        assertTrue(vm.actionState.value is WageActionState.Idle)
        job.cancel()
    }

    // ── H-1: Session expiry propagation via SessionEventBus ──────────────────

    @Test
    fun `WagesViewModel auth error on load emits session expired event`() = runTest(testDispatcher) {
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) =
                WageResult.Error("انتهت الجلسة", isAuth = true)
        }
        val events = mutableListOf<Unit>()
        val job = launch { SessionEventBus.sessionExpired.collect { events.add(it) } }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        assertTrue("Expected session expired event", events.isNotEmpty())
        // uiState stays as Loading (never set to Error) since auth error navigates away
        assertTrue(vm.uiState.value is WagesUiState.Loading)
        job.cancel()
    }

    @Test
    fun `WagesViewModel auth error on declarePayment emits session expired and resets actionState`() = runTest(testDispatcher) {
        val obligation = fakeObligation("obl-auth", "PENDING")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun declarePayment(id: String, request: DeclarePaymentRequest) =
                WageResult.Error("انتهت الجلسة", isAuth = true)
        }
        val events = mutableListOf<Unit>()
        val job = launch { SessionEventBus.sessionExpired.collect { events.add(it) } }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        vm.declarePayment("obl-auth", 10000L, "2026-10-11", "CASH", "")
        advanceUntilIdle()
        assertTrue("Expected session expired event", events.isNotEmpty())
        assertTrue(vm.actionState.value is WageActionState.Idle)
        job.cancel()
    }

    @Test
    fun `WorkerWagesViewModel auth error on acknowledgePayment emits session expired`() = runTest(testDispatcher) {
        val obligation = fakeObligation("w-auth-1", "AWAITING_ACK")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun acknowledgePayment(id: String) =
                WageResult.Error("غير مصرح", isAuth = true)
        }
        val events = mutableListOf<Unit>()
        val job = launch { SessionEventBus.sessionExpired.collect { events.add(it) } }
        val vm = WorkerWagesViewModel(repo)
        advanceUntilIdle()
        vm.acknowledgePayment("w-auth-1")
        advanceUntilIdle()
        assertTrue("Expected session expired event", events.isNotEmpty())
        job.cancel()
    }

    @Test
    fun `non-auth error does NOT emit session expired and shows error state`() = runTest(testDispatcher) {
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) =
                WageResult.Error("تعذّر الاتصال بالخادم", isAuth = false)
        }
        val events = mutableListOf<Unit>()
        val job = launch { SessionEventBus.sessionExpired.collect { events.add(it) } }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        assertTrue("Should not emit session expired for non-auth error", events.isEmpty())
        assertTrue(vm.uiState.value is WagesUiState.Error)
        job.cancel()
    }
}
