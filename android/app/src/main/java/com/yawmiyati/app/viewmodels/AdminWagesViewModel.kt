package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.AdminVerifyDeclarationRequest
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.data.wages.WageRepository
import com.yawmiyati.app.data.wages.WageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminWagesViewModel(
    private val repository: WageRepository = WageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WagesUiState>(WagesUiState.Loading)
    val uiState: StateFlow<WagesUiState> = _uiState

    private val _actionState = MutableStateFlow<WageActionState>(WageActionState.Idle)
    val actionState: StateFlow<WageActionState> = _actionState

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    init { loadPendingObligations() }

    fun loadPendingObligations() {
        viewModelScope.launch {
            _uiState.value = WagesUiState.Loading
            when (val r = repository.listObligations(status = "AWAITING_ACK")) {
                is WageResult.Success ->
                    _uiState.value = if (r.data.isEmpty()) WagesUiState.Empty
                                     else WagesUiState.Success(r.data)
                is WageResult.Error -> {
                    if (r.isAuth) _sessionExpired.value = true
                    else _uiState.value = WagesUiState.Error(r.message)
                }
            }
        }
    }

    fun verifyDeclaration(obligationId: String, declarationId: String, reviewNotes: String = "") {
        submitVerdict(obligationId, declarationId, action = "verify", reviewNotes = reviewNotes)
    }

    fun rejectDeclaration(obligationId: String, declarationId: String, reviewNotes: String) {
        submitVerdict(obligationId, declarationId, action = "reject", reviewNotes = reviewNotes)
    }

    private fun submitVerdict(obligationId: String, declarationId: String, action: String, reviewNotes: String) {
        if (_actionState.value is WageActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WageActionState.Loading
            val req = AdminVerifyDeclarationRequest(action = action, reviewNotes = reviewNotes)
            when (val r = repository.verifyDeclaration(obligationId, declarationId, req)) {
                is WageResult.Success -> {
                    _actionState.value = WageActionState.Success
                    removeObligation(obligationId)
                }
                is WageResult.Error -> {
                    if (r.isAuth) {
                        _actionState.value = WageActionState.Idle
                        _sessionExpired.value = true
                    } else {
                        _actionState.value = WageActionState.Error(r.message)
                    }
                }
            }
        }
    }

    private fun removeObligation(id: String) {
        val current = _uiState.value
        if (current is WagesUiState.Success) {
            val remaining = current.obligations.filter { it.id != id }
            _uiState.value = if (remaining.isEmpty()) WagesUiState.Empty else WagesUiState.Success(remaining)
        }
    }

    fun resetActionState() {
        _actionState.value = WageActionState.Idle
    }
}

/**
 * Returns the single declaration the admin should act on: the most recent SUBMITTED one.
 *
 * The backend serializes declarations newest-first (ordering = ["-declared_at"]).
 * After a rejection + re-declaration, the list contains both a REJECTED entry and a newer
 * SUBMITTED entry. Selecting by status rather than list position is the correct contract.
 */
internal fun selectReviewableDeclaration(
    declarations: List<com.yawmiyati.app.data.api.PaymentDeclarationResponse>
): com.yawmiyati.app.data.api.PaymentDeclarationResponse? =
    declarations.firstOrNull { it.status == "SUBMITTED" }
