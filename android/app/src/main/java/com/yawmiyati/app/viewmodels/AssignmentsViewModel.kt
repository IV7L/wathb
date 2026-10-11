package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.AssignmentResponse
import com.yawmiyati.app.data.assignments.AssignmentRepository
import com.yawmiyati.app.data.assignments.AssignmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AssignmentsUiState {
    object Loading : AssignmentsUiState()
    data class Success(val assignments: List<AssignmentResponse>) : AssignmentsUiState()
    data class Error(val message: String) : AssignmentsUiState()
    object Empty : AssignmentsUiState()
}

sealed class AssignmentActionState {
    object Idle : AssignmentActionState()
    object Loading : AssignmentActionState()
    data class Error(val message: String) : AssignmentActionState()
}

class AssignmentsViewModel(
    private val repository: AssignmentRepository = AssignmentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssignmentsUiState>(AssignmentsUiState.Loading)
    val uiState: StateFlow<AssignmentsUiState> = _uiState

    private val _actionState = MutableStateFlow<AssignmentActionState>(AssignmentActionState.Idle)
    val actionState: StateFlow<AssignmentActionState> = _actionState

    init { loadAssignments() }

    fun loadAssignments() {
        viewModelScope.launch {
            _uiState.value = AssignmentsUiState.Loading
            _uiState.value = when (val result = repository.listAssignments()) {
                is AssignmentResult.Success ->
                    if (result.data.isEmpty()) AssignmentsUiState.Empty
                    else AssignmentsUiState.Success(result.data)
                is AssignmentResult.Error -> AssignmentsUiState.Error(result.message)
            }
        }
    }

    fun acceptAssignment(id: String) = performAction(id, accept = true)
    fun declineAssignment(id: String) = performAction(id, accept = false)

    private fun performAction(id: String, accept: Boolean) {
        if (_actionState.value is AssignmentActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = AssignmentActionState.Loading
            val result = if (accept) repository.acceptAssignment(id) else repository.declineAssignment(id)
            when (result) {
                is AssignmentResult.Success -> {
                    _actionState.value = AssignmentActionState.Idle
                    val current = _uiState.value
                    if (current is AssignmentsUiState.Success) {
                        val updated = current.assignments.map {
                            if (it.id == result.data.id) result.data else it
                        }
                        _uiState.value = AssignmentsUiState.Success(updated)
                    }
                }
                is AssignmentResult.Error -> {
                    _actionState.value = AssignmentActionState.Error(result.message)
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = AssignmentActionState.Idle
    }
}
