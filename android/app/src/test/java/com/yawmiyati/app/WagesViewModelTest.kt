package com.yawmiyati.app

import com.yawmiyati.app.data.api.AdminVerifyDeclarationRequest
import com.yawmiyati.app.data.api.DeclarePaymentRequest
import com.yawmiyati.app.data.api.PaymentDeclarationResponse
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.data.wages.WageRepository
import com.yawmiyati.app.data.wages.WageResult
import com.yawmiyati.app.viewmodels.AdminWagesViewModel
import com.yawmiyati.app.viewmodels.WageActionState
import com.yawmiyati.app.viewmodels.WagesUiState
import com.yawmiyati.app.viewmodels.WagesViewModel
import com.yawmiyati.app.viewmodels.WorkerWagesViewModel
import com.yawmiyati.app.viewmodels.selectReviewableDeclaration
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
        id = id, assignment = "assign-$id", workerEmail = "worker@test.com",
        jobTitle = "Day Work", amount = 10000L, currency = "ILS",
        amountDisplay = "100 ILS", dueDate = "2026-10-15", status = status,
        createdAt = "2026-10-10T08:00:00Z", declarations = emptyList()
    )

    private fun fakeDeclaration(id: String, obligationId: String) = PaymentDeclarationResponse(
        id = id, wageObligation = obligationId, declaredAmount = 10000L,
        currency = "ILS", paymentDate = "2026-10-10", paymentMethod = "CASH",
        transactionReference = "", declaredByEmail = "biz@test.com",
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

    // ── AdminWagesViewModel ───────────────────────────────────────────────────

    @Test
    fun `admin loads only AWAITING_ACK obligations`() = runTest(testDispatcher) {
        val pending = listOf(fakeObligation("a-1", "AWAITING_ACK"))
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) =
                if (status == "AWAITING_ACK") WageResult.Success(pending)
                else WageResult.Success(emptyList())
        }
        val vm = AdminWagesViewModel(repo)
        advanceUntilIdle()
        val state = vm.uiState.value as WagesUiState.Success
        assertEquals(1, state.obligations.size)
        assertEquals("AWAITING_ACK", state.obligations.first().status)
    }

    @Test
    fun `admin verifyDeclaration removes obligation from list`() = runTest(testDispatcher) {
        val obligation = fakeObligation("a-2", "AWAITING_ACK")
        val declaration = fakeDeclaration("d-2", "a-2").copy(status = "VERIFIED")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun verifyDeclaration(
                obligationId: String, declarationId: String, request: AdminVerifyDeclarationRequest
            ) = WageResult.Success(declaration)
        }
        val vm = AdminWagesViewModel(repo)
        advanceUntilIdle()
        vm.verifyDeclaration("a-2", "d-2")
        advanceUntilIdle()
        assertEquals(WageActionState.Success, vm.actionState.value)
        assertTrue(vm.uiState.value is WagesUiState.Empty)
    }

    // ── selectReviewableDeclaration ───────────────────────────────────────────

    @Test
    fun `selectReviewableDeclaration returns null for empty list`() {
        assertNull(selectReviewableDeclaration(emptyList()))
    }

    @Test
    fun `selectReviewableDeclaration returns the single SUBMITTED declaration`() {
        val decl = fakeDeclaration("d-sub", "obl-1").copy(status = "SUBMITTED")
        assertEquals(decl, selectReviewableDeclaration(listOf(decl)))
    }

    @Test
    fun `selectReviewableDeclaration returns null when only REJECTED declarations exist`() {
        val rejected = fakeDeclaration("d-rej", "obl-1").copy(status = "REJECTED")
        assertNull(selectReviewableDeclaration(listOf(rejected)))
    }

    @Test
    fun `selectReviewableDeclaration returns SUBMITTED when list is newest-first rejected then submitted`() {
        // Backend ordering is -declared_at: newest first.
        // Real scenario from F.1: first declare rejected, then re-declared as submitted.
        // API returns submitted (newer) at index 0, rejected (older) at index 1.
        val submitted = fakeDeclaration("d-new", "obl-1").copy(status = "SUBMITTED")
        val rejected  = fakeDeclaration("d-old", "obl-1").copy(status = "REJECTED")
        val result = selectReviewableDeclaration(listOf(submitted, rejected))
        assertEquals(submitted, result)
        assertEquals("SUBMITTED", result?.status)
    }

    @Test
    fun `selectReviewableDeclaration returns SUBMITTED regardless of position in list`() {
        // Guard against ordering assumptions: SUBMITTED appears last in the list.
        val rejected  = fakeDeclaration("d-first", "obl-1").copy(status = "REJECTED")
        val submitted = fakeDeclaration("d-last",  "obl-1").copy(status = "SUBMITTED")
        val result = selectReviewableDeclaration(listOf(rejected, submitted))
        assertEquals(submitted, result)
    }

    @Test
    fun `admin rejectDeclaration also removes obligation from list`() = runTest(testDispatcher) {
        val obligation = fakeObligation("a-3", "AWAITING_ACK")
        val declaration = fakeDeclaration("d-3", "a-3").copy(status = "REJECTED")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun verifyDeclaration(
                obligationId: String, declarationId: String, request: AdminVerifyDeclarationRequest
            ) = WageResult.Success(declaration)
        }
        val vm = AdminWagesViewModel(repo)
        advanceUntilIdle()
        vm.rejectDeclaration("a-3", "d-3", "غير واضح")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is WagesUiState.Empty)
    }

    // ── H-1: Session expiry propagation ──────────────────────────────────────

    @Test
    fun `WagesViewModel auth error on load sets sessionExpired`() = runTest(testDispatcher) {
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) =
                WageResult.Error("انتهت الجلسة", isAuth = true)
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.sessionExpired.value)
        assertTrue(vm.uiState.value is WagesUiState.Loading) // stays Loading, not Error
    }

    @Test
    fun `WagesViewModel auth error on declarePayment sets sessionExpired`() = runTest(testDispatcher) {
        val obligation = fakeObligation("obl-auth", "PENDING")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun declarePayment(id: String, request: DeclarePaymentRequest) =
                WageResult.Error("انتهت الجلسة", isAuth = true)
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        vm.declarePayment("obl-auth", 10000L, "2026-10-11", "CASH", "")
        advanceUntilIdle()
        assertTrue(vm.sessionExpired.value)
        assertTrue(vm.actionState.value is WageActionState.Idle) // no error shown
    }

    @Test
    fun `WorkerWagesViewModel auth error on acknowledgePayment sets sessionExpired`() = runTest(testDispatcher) {
        val obligation = fakeObligation("w-auth-1", "AWAITING_ACK")
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) = WageResult.Success(listOf(obligation))
            override suspend fun acknowledgePayment(id: String) =
                WageResult.Error("غير مصرح", isAuth = true)
        }
        val vm = WorkerWagesViewModel(repo)
        advanceUntilIdle()
        vm.acknowledgePayment("w-auth-1")
        advanceUntilIdle()
        assertTrue(vm.sessionExpired.value)
    }

    @Test
    fun `AdminWagesViewModel auth error on load sets sessionExpired`() = runTest(testDispatcher) {
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) =
                WageResult.Error("Unauthorized", isAuth = true)
        }
        val vm = AdminWagesViewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.sessionExpired.value)
    }

    @Test
    fun `non-auth error does NOT set sessionExpired`() = runTest(testDispatcher) {
        val repo = object : WageRepository() {
            override suspend fun listObligations(status: String?) =
                WageResult.Error("تعذّر الاتصال بالخادم", isAuth = false)
        }
        val vm = WagesViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.sessionExpired.value)
        assertTrue(vm.uiState.value is WagesUiState.Error)
    }
}
