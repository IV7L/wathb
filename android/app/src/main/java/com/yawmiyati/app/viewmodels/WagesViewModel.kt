package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.DeclarePaymentRequest
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.data.wages.WageRepository
import com.yawmiyati.app.data.wages.WageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WagesUiState {
    object Loading : WagesUiState()
    data class Success(val obligations: List<WageObligationResponse>) : WagesUiState()
    data class Error(val message: String) : WagesUiState()
    object Empty : WagesUiState()
}

sealed class WageActionState {
    object Idle : WageActionState()
    object Loading : WageActionState()
    object Success : WageActionState()
    data class Error(val message: String) : WageActionState()
}

class WagesViewModel(
    private val repository: WageRepository = WageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WagesUiState>(WagesUiState.Loading)
    val uiState: StateFlow<WagesUiState> = _uiState

    private val _actionState = MutableStateFlow<WageActionState>(WageActionState.Idle)
    val actionState: StateFlow<WageActionState> = _actionState

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired

    init { loadObligations() }

    fun loadObligations() {
        viewModelScope.launch {
            _uiState.value = WagesUiState.Loading
            when (val r = repository.listObligations()) {
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

    fun declarePayment(
        obligationId: String,
        declaredAmount: Long,
        paymentDate: String,
        paymentMethod: String,
        transactionReference: String
    ) {
        if (_actionState.value is WageActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WageActionState.Loading
            val req = DeclarePaymentRequest(
                declaredAmount = declaredAmount,
                paymentDate = paymentDate,
                paymentMethod = paymentMethod,
                transactionReference = transactionReference
            )
            when (val r = repository.declarePayment(obligationId, req)) {
                is WageResult.Success -> {
                    _actionState.value = WageActionState.Success
                    updateObligationStatus(obligationId, "AWAITING_ACK")
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

    private fun updateObligationStatus(id: String, newStatus: String) {
        val current = _uiState.value
        if (current is WagesUiState.Success) {
            val updated = current.obligations.map {
                if (it.id == id) it.copy(status = newStatus) else it
            }
            _uiState.value = WagesUiState.Success(updated)
        }
    }

    fun resetActionState() {
        _actionState.value = WageActionState.Idle
    }
}
