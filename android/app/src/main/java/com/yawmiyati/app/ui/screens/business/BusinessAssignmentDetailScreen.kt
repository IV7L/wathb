package com.yawmiyati.app.ui.screens.business

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.AttendanceResponse
import com.yawmiyati.app.data.api.CompletionResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AssignmentWorkActionState
import com.yawmiyati.app.viewmodels.AttendanceUiState
import com.yawmiyati.app.viewmodels.BusinessAssignmentViewModel
import com.yawmiyati.app.viewmodels.CompletionUiState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAssignmentDetailScreen(
    assignmentId: String,
    workerEmail: String,
    onBack: () -> Unit,
    viewModel: BusinessAssignmentViewModel = viewModel()
) {
    val attendanceState by viewModel.attendanceState.collectAsState()
    val completionState by viewModel.completionState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var attendanceNotes by rememberSaveable { mutableStateOf("") }
    var completionHours by rememberSaveable { mutableStateOf("") }
    var completionNotes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(assignmentId) { viewModel.load(assignmentId) }

    LaunchedEffect(actionState) {
        when (actionState) {
            is AssignmentWorkActionState.Success -> {
                snackbarHostState.showSnackbar("تم الحفظ بنجاح")
                viewModel.resetActionState()
            }
            is AssignmentWorkActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as AssignmentWorkActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    val isActionLoading = actionState is AssignmentWorkActionState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة المهمة — $workerEmail") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Attendance section ────────────────────────────────────────────
            item {
                Text("الحضور", style = MaterialTheme.typography.titleMedium)
            }

            when (val att = attendanceState) {
                is AttendanceUiState.Loading -> item { LoadingIndicator() }
                is AttendanceUiState.Empty   -> item {
                    Text("لا توجد سجلات حضور بعد", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is AttendanceUiState.Error   -> item { ErrorMessage(message = att.message) }
                is AttendanceUiState.Success -> items(att.records, key = { it.id }) { record ->
                    AttendanceCard(
                        record = record,
                        isLoading = isActionLoading,
                        onConfirm = { viewModel.confirmAttendance(assignmentId, record.id) }
                    )
                }
            }

            // Submit attendance form
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تسجيل حضور يوم العمل", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = attendanceNotes,
                            onValueChange = { attendanceNotes = it },
                            label = { Text("ملاحظات (اختياري)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val today = LocalDate.now().toString()
                                viewModel.submitAttendance(assignmentId, today, attendanceNotes)
                                attendanceNotes = ""
                            },
                            enabled = !isActionLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تسجيل الحضور ليوم اليوم")
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            // ── Completion section ────────────────────────────────────────────
            item {
                Text("إتمام المهمة", style = MaterialTheme.typography.titleMedium)
            }

            when (val comp = completionState) {
                is CompletionUiState.Loading      -> item { LoadingIndicator() }
                is CompletionUiState.NotSubmitted -> item {
                    // Show submit completion form
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("تقديم سجل الإتمام", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = completionHours,
                                onValueChange = { completionHours = it },
                                label = { Text("عدد ساعات العمل (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = completionNotes,
                                onValueChange = { completionNotes = it },
                                label = { Text("ملاحظات (اختياري)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    viewModel.submitCompletion(assignmentId, completionHours, completionNotes)
                                },
                                enabled = !isActionLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تقديم إتمام المهمة")
                            }
                        }
                    }
                }
                is CompletionUiState.Submitted    -> item {
                    CompletionCard(
                        record = comp.record,
                        isLoading = isActionLoading,
                        onConfirm = { viewModel.confirmCompletion(assignmentId) }
                    )
                }
                is CompletionUiState.Error        -> item { ErrorMessage(message = comp.message) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AttendanceCard(
    record: AttendanceResponse,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.workDate, style = MaterialTheme.typography.bodyMedium)
                AttendanceStatusBadge(record.status)
            }
            Text("بواسطة: ${record.submittedByEmail}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.status == "SUBMITTED") {
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد الحضور")
                }
            }
        }
    }
}

@Composable
private fun CompletionCard(
    record: CompletionResponse,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("سجل الإتمام", style = MaterialTheme.typography.titleSmall)
                CompletionStatusBadge(record.status)
            }
            if (!record.hoursWorked.isNullOrBlank()) {
                Text("ساعات العمل: ${record.hoursWorked}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("بواسطة: ${record.submittedByEmail}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.status == "SUBMITTED") {
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد الإتمام")
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatusBadge(status: String) {
    val (label, color) = when (status) {
        "SUBMITTED"  -> "مقدّم"      to MaterialTheme.colorScheme.secondary
        "CONFIRMED"  -> "مؤكد"       to MaterialTheme.colorScheme.tertiary
        "DISPUTED"   -> "متنازع عليه" to MaterialTheme.colorScheme.error
        "CANCELLED"  -> "ملغى"       to MaterialTheme.colorScheme.error
        else         -> status        to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun CompletionStatusBadge(status: String) {
    val (label, color) = when (status) {
        "SUBMITTED"  -> "مقدّم"    to MaterialTheme.colorScheme.secondary
        "CONFIRMED"  -> "مؤكد"     to MaterialTheme.colorScheme.tertiary
        "DISPUTED"   -> "متنازع عليه" to MaterialTheme.colorScheme.error
        else         -> status      to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
