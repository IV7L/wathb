package com.yawmiyati.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yawmiyati.app.data.api.AttendanceResponse
import com.yawmiyati.app.data.api.CompletionResponse
import com.yawmiyati.app.data.assignments.AssignmentRepository
import com.yawmiyati.app.data.assignments.AssignmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WorkerAttendanceUiState {
    object Loading : WorkerAttendanceUiState()
    object Empty : WorkerAttendanceUiState()
    data class Success(val records: List<AttendanceResponse>) : WorkerAttendanceUiState()
    data class Error(val message: String) : WorkerAttendanceUiState()
}

sealed class WorkerCompletionUiState {
    object Loading : WorkerCompletionUiState()
    object NotSubmitted : WorkerCompletionUiState()
    data class Submitted(val record: CompletionResponse) : WorkerCompletionUiState()
    data class Error(val message: String) : WorkerCompletionUiState()
}

sealed class WorkerAssignmentActionState {
    object Idle : WorkerAssignmentActionState()
    object Loading : WorkerAssignmentActionState()
    object Success : WorkerAssignmentActionState()
    data class Error(val message: String) : WorkerAssignmentActionState()
}

class WorkerAssignmentDetailViewModel(
    private val repository: AssignmentRepository = AssignmentRepository()
) : ViewModel() {

    private val _attendanceState = MutableStateFlow<WorkerAttendanceUiState>(WorkerAttendanceUiState.Loading)
    val attendanceState: StateFlow<WorkerAttendanceUiState> = _attendanceState

    private val _completionState = MutableStateFlow<WorkerCompletionUiState>(WorkerCompletionUiState.Loading)
    val completionState: StateFlow<WorkerCompletionUiState> = _completionState

    private val _actionState = MutableStateFlow<WorkerAssignmentActionState>(WorkerAssignmentActionState.Idle)
    val actionState: StateFlow<WorkerAssignmentActionState> = _actionState

    private var currentAssignmentId: String? = null

    fun load(assignmentId: String) {
        if (currentAssignmentId == assignmentId) return
        currentAssignmentId = assignmentId
        loadAttendance(assignmentId)
        loadCompletion(assignmentId)
    }

    private fun loadAttendance(assignmentId: String) {
        viewModelScope.launch {
            _attendanceState.value = WorkerAttendanceUiState.Loading
            when (val r = repository.listAttendance(assignmentId)) {
                is AssignmentResult.Success ->
                    _attendanceState.value = if (r.data.isEmpty()) WorkerAttendanceUiState.Empty
                                             else WorkerAttendanceUiState.Success(r.data)
                is AssignmentResult.Error ->
                    _attendanceState.value = WorkerAttendanceUiState.Error(r.message)
            }
        }
    }

    private fun loadCompletion(assignmentId: String) {
        viewModelScope.launch {
            _completionState.value = WorkerCompletionUiState.Loading
            when (val r = repository.getCompletion(assignmentId)) {
                is AssignmentResult.Success ->
                    _completionState.value = WorkerCompletionUiState.Submitted(r.data)
                is AssignmentResult.Error ->
                    _completionState.value = if (r.message.contains("لا يوجد سجل"))
                        WorkerCompletionUiState.NotSubmitted
                    else WorkerCompletionUiState.Error(r.message)
            }
        }
    }

    fun confirmAttendance(assignmentId: String, recId: String) = performAction {
        repository.confirmAttendance(assignmentId, recId).also { result ->
            if (result is AssignmentResult.Success) {
                updateAttendanceRecord(result.data)
            }
        }
    }

    fun disputeAttendance(assignmentId: String, recId: String) = performAction {
        repository.disputeAttendance(assignmentId, recId).also { result ->
            if (result is AssignmentResult.Success) {
                updateAttendanceRecord(result.data)
            }
        }
    }

    fun confirmCompletion(assignmentId: String) = performAction {
        repository.confirmCompletion(assignmentId).also { result ->
            if (result is AssignmentResult.Success) {
                _completionState.value = WorkerCompletionUiState.Submitted(result.data)
            }
        }
    }

    fun disputeCompletion(assignmentId: String) = performAction {
        repository.disputeCompletion(assignmentId).also { result ->
            if (result is AssignmentResult.Success) {
                _completionState.value = WorkerCompletionUiState.Submitted(result.data)
            }
        }
    }

    private fun performAction(block: suspend () -> AssignmentResult<*>) {
        if (_actionState.value is WorkerAssignmentActionState.Loading) return
        viewModelScope.launch {
            _actionState.value = WorkerAssignmentActionState.Loading
            val result = block()
            _actionState.value = when (result) {
                is AssignmentResult.Success -> WorkerAssignmentActionState.Success
                is AssignmentResult.Error   -> WorkerAssignmentActionState.Error(result.message)
            }
        }
    }

    private fun updateAttendanceRecord(updated: AttendanceResponse) {
        val current = _attendanceState.value
        if (current is WorkerAttendanceUiState.Success) {
            _attendanceState.value = WorkerAttendanceUiState.Success(
                current.records.map { if (it.id == updated.id) updated else it }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = WorkerAssignmentActionState.Idle
    }
}
