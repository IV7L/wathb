package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.AttendanceResponse
import com.yawmiyati.app.data.api.AttendanceSubmitRequest
import com.yawmiyati.app.data.api.CompletionResponse
import com.yawmiyati.app.data.api.CompletionSubmitRequest
import com.yawmiyati.app.data.assignments.AssignmentRepository
import com.yawmiyati.app.data.assignments.AssignmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AttendanceUiState {
    object Loading : AttendanceUiState()
    data class Success(val records: List<AttendanceResponse>) : AttendanceUiState()
    data class Error(val message: String) : AttendanceUiState()
    object Empty : AttendanceUiState()
}

sealed class CompletionUiState {
    object Loading : CompletionUiState()
    object NotSubmitted : CompletionUiState()
    data class Submitted(val record: CompletionResponse) : CompletionUiState()
    data class Error(val message: String) : CompletionUiState()
}

sealed class AssignmentWorkActionState {
    object Idle : AssignmentWorkActionState()
    object Loading : AssignmentWorkActionState()
    object Success : AssignmentWorkActionState()
    data class Error(val message: String) : AssignmentWorkActionState()
}

class BusinessAssignmentViewModel(
    private val repository: AssignmentRepository = AssignmentRepository()
) : ViewModel() {

    private val _attendanceState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val attendanceState: StateFlow<AttendanceUiState> = _attendanceState

    private val _completionState = MutableStateFlow<CompletionUiState>(CompletionUiState.Loading)
    val completionState: StateFlow<CompletionUiState> = _completionState

    private val _actionState = MutableStateFlow<AssignmentWorkActionState>(AssignmentWorkActionState.Idle)
    val actionState: StateFlow<AssignmentWorkActionState> = _actionState

    fun load(assignmentId: String) {
        loadAttendance(assignmentId)
        loadCompletion(assignmentId)
    }

    private fun loadAttendance(assignmentId: String) {
        viewModelScope.launch {
            _attendanceState.value = AttendanceUiState.Loading
            _attendanceState.value = when (val r = repository.listAttendance(assignmentId)) {
                is AssignmentResult.Success ->
                    if (r.data.isEmpty()) AttendanceUiState.Empty
                    else AttendanceUiState.Success(r.data)
                is AssignmentResult.Error -> AttendanceUiState.Error(r.message)
            }
        }
    }

    private fun loadCompletion(assignmentId: String) {
        viewModelScope.launch {
            _completionState.value = CompletionUiState.Loading
            _completionState.value = when (val r = repository.getCompletion(assignmentId)) {
                is AssignmentResult.Success -> CompletionUiState.Submitted(r.data)
                is AssignmentResult.Error ->
                    if (r.message.contains("لا يوجد")) CompletionUiState.NotSubmitted
                    else CompletionUiState.Error(r.message)
            }
        }
    }

    fun submitAttendance(assignmentId: String, workDate: String, notes: String) {
        if (_actionState.value is AssignmentWorkActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = AssignmentWorkActionState.Loading
            val req = AttendanceSubmitRequest(workDate = workDate, notes = notes)
            when (val r = repository.submitAttendance(assignmentId, req)) {
                is AssignmentResult.Success -> {
                    _actionState.value = AssignmentWorkActionState.Success
                    loadAttendance(assignmentId)
                }
                is AssignmentResult.Error -> _actionState.value = AssignmentWorkActionState.Error(r.message)
            }
        }
    }

    fun confirmAttendance(assignmentId: String, recId: String) {
        if (_actionState.value is AssignmentWorkActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = AssignmentWorkActionState.Loading
            when (val r = repository.confirmAttendance(assignmentId, recId)) {
                is AssignmentResult.Success -> {
                    _actionState.value = AssignmentWorkActionState.Success
                    loadAttendance(assignmentId)
                }
                is AssignmentResult.Error -> _actionState.value = AssignmentWorkActionState.Error(r.message)
            }
        }
    }

    fun submitCompletion(assignmentId: String, hoursWorked: String?, notes: String) {
        if (_actionState.value is AssignmentWorkActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = AssignmentWorkActionState.Loading
            val req = CompletionSubmitRequest(
                hoursWorked = hoursWorked?.takeIf { it.isNotBlank() },
                notes = notes
            )
            when (val r = repository.submitCompletion(assignmentId, req)) {
                is AssignmentResult.Success -> {
                    _actionState.value = AssignmentWorkActionState.Success
                    _completionState.value = CompletionUiState.Submitted(r.data)
                }
                is AssignmentResult.Error -> _actionState.value = AssignmentWorkActionState.Error(r.message)
            }
        }
    }

    fun confirmCompletion(assignmentId: String) {
        if (_actionState.value is AssignmentWorkActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = AssignmentWorkActionState.Loading
            when (val r = repository.confirmCompletion(assignmentId)) {
                is AssignmentResult.Success -> {
                    _actionState.value = AssignmentWorkActionState.Success
                    _completionState.value = CompletionUiState.Submitted(r.data)
                }
                is AssignmentResult.Error -> _actionState.value = AssignmentWorkActionState.Error(r.message)
            }
        }
    }

    fun resetActionState() {
        _actionState.value = AssignmentWorkActionState.Idle
    }
}
