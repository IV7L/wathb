package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.ApiClient
import com.yawmiyati.app.data.api.DisputeResponse
import com.yawmiyati.app.data.api.DisputeResolveRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DisputesUiState {
    object Loading : DisputesUiState()
    object Empty : DisputesUiState()
    data class Success(val disputes: List<DisputeResponse>) : DisputesUiState()
    data class Error(val message: String) : DisputesUiState()
}

sealed class DisputeActionState {
    object Idle : DisputeActionState()
    object Loading : DisputeActionState()
    object Success : DisputeActionState()
    data class Error(val message: String) : DisputeActionState()
}

class AdminDisputesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DisputesUiState>(DisputesUiState.Loading)
    val uiState: StateFlow<DisputesUiState> = _uiState

    private val _actionState = MutableStateFlow<DisputeActionState>(DisputeActionState.Idle)
    val actionState: StateFlow<DisputeActionState> = _actionState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DisputesUiState.Loading
            try {
                val resp = ApiClient.adminApi.listDisputes()
                if (resp.isSuccessful) {
                    val list = resp.body()?.results ?: emptyList()
                    _uiState.value = if (list.isEmpty()) DisputesUiState.Empty
                                     else DisputesUiState.Success(list)
                } else {
                    _uiState.value = DisputesUiState.Error("خطأ ${resp.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = DisputesUiState.Error(e.message ?: "خطأ في الاتصال")
            }
        }
    }

    fun resolve(disputeId: String, action: String, notes: String) {
        viewModelScope.launch {
            _actionState.value = DisputeActionState.Loading
            try {
                val resp = ApiClient.adminApi.resolveDispute(disputeId, DisputeResolveRequest(action, notes))
                if (resp.isSuccessful) {
                    _actionState.value = DisputeActionState.Success
                    load()
                } else {
                    _actionState.value = DisputeActionState.Error("خطأ ${resp.code()}")
                }
            } catch (e: Exception) {
                _actionState.value = DisputeActionState.Error(e.message ?: "خطأ في الاتصال")
            }
        }
    }

    fun resetActionState() { _actionState.value = DisputeActionState.Idle }
}
