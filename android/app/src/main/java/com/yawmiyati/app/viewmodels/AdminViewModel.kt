package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.admin.AdminRepository
import com.yawmiyati.app.data.admin.AdminResult
import com.yawmiyati.app.data.api.AdminBusinessProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AdminUiState {
    object Loading : AdminUiState()
    data class Success(val businesses: List<AdminBusinessProfile>) : AdminUiState()
    data class Error(val message: String) : AdminUiState()
    object Empty : AdminUiState()
}

sealed class VerifyActionState {
    object Idle : VerifyActionState()
    object Loading : VerifyActionState()
    data class Error(val message: String) : VerifyActionState()
}

class AdminViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState

    private val _actionState = MutableStateFlow<VerifyActionState>(VerifyActionState.Idle)
    val actionState: StateFlow<VerifyActionState> = _actionState

    private var currentFilter: String? = null

    init {
        loadBusinesses()
    }

    fun loadBusinesses(statusFilter: String? = currentFilter) {
        currentFilter = statusFilter
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            _uiState.value = when (val result = repository.listBusinesses(statusFilter)) {
                is AdminResult.Success -> if (result.data.isEmpty()) AdminUiState.Empty
                                         else AdminUiState.Success(result.data)
                is AdminResult.Error   -> AdminUiState.Error(result.message)
            }
        }
    }

    fun verifyBusiness(pk: Int, action: String, reason: String = "") {
        if (_actionState.value is VerifyActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = VerifyActionState.Loading
            when (val result = repository.verifyBusiness(pk, action, reason)) {
                is AdminResult.Success -> {
                    _actionState.value = VerifyActionState.Idle
                    // Refresh the list so the updated status is visible.
                    loadBusinesses(currentFilter)
                }
                is AdminResult.Error -> {
                    _actionState.value = VerifyActionState.Error(result.message)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = VerifyActionState.Idle
    }
}
