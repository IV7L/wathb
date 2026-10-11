package com.yawmiyati.app.ui.screens.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.AttendanceResponse
import com.yawmiyati.app.data.api.CompletionResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.WorkerAssignmentActionState
import com.yawmiyati.app.viewmodels.WorkerAssignmentDetailViewModel
import com.yawmiyati.app.viewmodels.WorkerAttendanceUiState
import com.yawmiyati.app.viewmodels.WorkerCompletionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerAssignmentDetailScreen(
    assignmentId: String,
    onBack: () -> Unit,
    viewModel: WorkerAssignmentDetailViewModel = viewModel()
) {
    val attendanceState by viewModel.attendanceState.collectAsState()
    val completionState by viewModel.completionState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(assignmentId) { viewModel.load(assignmentId) }

    LaunchedEffect(actionState) {
        when (actionState) {
            is WorkerAssignmentActionState.Success -> {
                snackbarHostState.showSnackbar("تم الإجراء بنجاح")
                viewModel.resetActionState()
            }
            is WorkerAssignmentActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as WorkerAssignmentActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    val isActionLoading = actionState is WorkerAssignmentActionState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل المهمة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Attendance ────────────────────────────────────────────────────
            item { Text("سجلات الحضور", style = MaterialTheme.typography.titleMedium) }

            when (val att = attendanceState) {
                is WorkerAttendanceUiState.Loading -> item { LoadingIndicator() }
                is WorkerAttendanceUiState.Empty   -> item {
                    EmptyState(
                        message = "لم يُسجّل صاحب العمل أي حضور بعد",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is WorkerAttendanceUiState.Error   -> item { ErrorMessage(message = att.message) }
                is WorkerAttendanceUiState.Success -> items(att.records, key = { it.id }) { record ->
                    WorkerAttendanceCard(
                        record = record,
                        isLoading = isActionLoading,
                        onConfirm = { viewModel.confirmAttendance(assignmentId, record.id) },
                        onDispute = { viewModel.disputeAttendance(assignmentId, record.id) }
                    )
                }
            }

            item { HorizontalDivider() }

            // ── Completion ────────────────────────────────────────────────────
            item { Text("إتمام المهمة", style = MaterialTheme.typography.titleMedium) }

            when (val comp = completionState) {
                is WorkerCompletionUiState.Loading       -> item { LoadingIndicator() }
                is WorkerCompletionUiState.NotSubmitted  -> item {
                    EmptyState(
                        message = "لم يُقدّم صاحب العمل سجل الإتمام بعد",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is WorkerCompletionUiState.Submitted     -> item {
                    WorkerCompletionCard(
                        record = comp.record,
                        isLoading = isActionLoading,
                        onConfirm = { viewModel.confirmCompletion(assignmentId) },
                        onDispute = { viewModel.disputeCompletion(assignmentId) }
                    )
                }
                is WorkerCompletionUiState.Error         -> item { ErrorMessage(message = comp.message) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WorkerAttendanceCard(
    record: AttendanceResponse,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDispute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.workDate, style = MaterialTheme.typography.bodyMedium)
                AttendanceBadge(record.status)
            }
            Text(
                text = "بواسطة: ${record.submittedByEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.status == "SUBMITTED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) { Text("تأكيد الحضور") }
                    OutlinedButton(
                        onClick = onDispute,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("الاعتراض") }
                }
            }
        }
    }
}

@Composable
private fun WorkerCompletionCard(
    record: CompletionResponse,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDispute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("سجل الإتمام", style = MaterialTheme.typography.titleSmall)
                CompletionBadge(record.status)
            }
            if (!record.hoursWorked.isNullOrBlank()) {
                Text(
                    text = "ساعات العمل: ${record.hoursWorked}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "بواسطة: ${record.submittedByEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.status == "SUBMITTED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) { Text("تأكيد الإتمام") }
                    OutlinedButton(
                        onClick = onDispute,
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("الاعتراض") }
                }
            }
        }
    }
}

@Composable
private fun AttendanceBadge(status: String) {
    val (label, color) = when (status) {
        "SUBMITTED" -> "مقدّم"       to MaterialTheme.colorScheme.secondary
        "CONFIRMED" -> "مؤكد"        to MaterialTheme.colorScheme.tertiary
        "DISPUTED"  -> "متنازع عليه" to MaterialTheme.colorScheme.error
        "CANCELLED" -> "ملغى"        to MaterialTheme.colorScheme.error
        else        -> status         to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CompletionBadge(status: String) {
    val (label, color) = when (status) {
        "SUBMITTED" -> "مقدّم"       to MaterialTheme.colorScheme.secondary
        "CONFIRMED" -> "مؤكد"        to MaterialTheme.colorScheme.tertiary
        "DISPUTED"  -> "متنازع عليه" to MaterialTheme.colorScheme.error
        else        -> status         to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
