package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.data.auth.SessionEventBus
import com.yawmiyati.app.data.wages.WageRepository
import com.yawmiyati.app.data.wages.WageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkerWagesViewModel(
    private val repository: WageRepository = WageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WagesUiState>(WagesUiState.Loading)
    val uiState: StateFlow<WagesUiState> = _uiState

    private val _actionState = MutableStateFlow<WageActionState>(WageActionState.Idle)
    val actionState: StateFlow<WageActionState> = _actionState

    init { loadObligations() }

    fun loadObligations() {
        viewModelScope.launch {
            _uiState.value = WagesUiState.Loading
            when (val r = repository.listObligations()) {
                is WageResult.Success ->
                    _uiState.value = if (r.data.isEmpty()) WagesUiState.Empty
                                     else WagesUiState.Success(r.data)
                is WageResult.Error -> {
                    if (r.isAuth) SessionEventBus.emitSessionExpired()
                    else _uiState.value = WagesUiState.Error(r.message)
                }
            }
        }
    }

    fun acknowledgePayment(obligationId: String) {
        if (_actionState.value is WageActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WageActionState.Loading
            when (val r = repository.acknowledgePayment(obligationId)) {
                is WageResult.Success -> {
                    _actionState.value = WageActionState.Success
                    replaceObligation(r.data)
                }
                is WageResult.Error -> {
                    if (r.isAuth) {
                        _actionState.value = WageActionState.Idle
                        SessionEventBus.emitSessionExpired()
                    } else {
                        _actionState.value = WageActionState.Error(r.message)
                    }
                }
            }
        }
    }

    private fun replaceObligation(updated: WageObligationResponse) {
        val current = _uiState.value
        if (current is WagesUiState.Success) {
            _uiState.value = WagesUiState.Success(
                current.obligations.map { if (it.id == updated.id) updated else it }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = WageActionState.Idle
    }
}
