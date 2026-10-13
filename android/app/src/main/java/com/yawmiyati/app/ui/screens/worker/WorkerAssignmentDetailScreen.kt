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
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.AttendanceResponse
import com.yawmiyati.app.data.api.CompletionResponse
import com.yawmiyati.app.ui.components.ConfirmCancelDialog
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.WorkerAssignmentActionState
import com.yawmiyati.app.viewmodels.WorkerAssignmentDetailViewModel
import com.yawmiyati.app.viewmodels.WorkerAttendanceUiState
import com.yawmiyati.app.viewmodels.WorkerCompletionUiState
import java.time.LocalDate

private fun hoursUntilAutoConfirm(submittedAt: String): Long {
    return try {
        val submitted = java.time.OffsetDateTime.parse(submittedAt)
        val deadline = submitted.plusHours(24)
        val remaining = java.time.Duration.between(java.time.OffsetDateTime.now(), deadline).toHours()
        remaining.coerceIn(0L, 23L)
    } catch (e: Exception) { 0L }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerAssignmentDetailScreen(
    assignmentId: String,
    workerPhone: String = "",
    businessPhone: String = "",
    assignmentStatus: String = "",
    onBack: () -> Unit,
    viewModel: WorkerAssignmentDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val attendanceState by viewModel.attendanceState.collectAsState()
    val completionState by viewModel.completionState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }
    var attendanceNotes by rememberSaveable { mutableStateOf("") }
    var completionHours by rememberSaveable { mutableStateOf("") }
    var completionNotes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(assignmentId) { viewModel.load(assignmentId) }

    LaunchedEffect(actionState) {
        when (actionState) {
            is WorkerAssignmentActionState.Success -> {
                showCancelDialog = false
                snackbarHostState.showSnackbar("تم الإجراء بنجاح")
                viewModel.resetActionState()
            }
            is WorkerAssignmentActionState.Error -> {
                showCancelDialog = false
                snackbarHostState.showSnackbar((actionState as WorkerAssignmentActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    val isActionLoading = actionState is WorkerAssignmentActionState.Loading

    if (showCancelDialog) {
        ConfirmCancelDialog(
            title = "إلغاء المهمة",
            body = "هل أنت متأكد أنك تريد إلغاء هذه المهمة؟ لا يمكن التراجع عن هذا الإجراء.",
            isLoading = isActionLoading,
            onConfirm = { reason -> viewModel.cancelAssignment(assignmentId, reason) },
            onDismiss = { showCancelDialog = false }
        )
    }

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

            // ── Cancel (ACCEPTED only) ────────────────────────────────────────
            if (assignmentStatus == "ACCEPTED") {
                item {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        enabled = !isActionLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error
                        )
                    ) { Text("إلغاء المهمة") }
                }
                item { HorizontalDivider() }
            }

            // ── Contact business ──────────────────────────────────────────────
            if (businessPhone.isNotBlank()) {
                item {
                    val localPhone = if (businessPhone.startsWith("+970"))
                        "0" + businessPhone.removePrefix("+970")
                    else businessPhone
                    val waPhone = businessPhone.replace("+", "")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$localPhone"))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("اتصل بصاحب العمل") }
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waPhone"))
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("واتساب") }
                    }
                }
                item { HorizontalDivider() }
            }

            // ── Attendance ────────────────────────────────────────────────────
            item { Text("سجلات الحضور", style = MaterialTheme.typography.titleMedium) }

            when (val att = attendanceState) {
                is WorkerAttendanceUiState.Loading -> item { LoadingIndicator() }
                is WorkerAttendanceUiState.Empty,
                is WorkerAttendanceUiState.Success -> {
                    if (att is WorkerAttendanceUiState.Success) {
                        items(att.records, key = { it.id }) { record ->
                            WorkerAttendanceCard(
                                record = record,
                                isLoading = isActionLoading,
                                isSelfSubmitted = record.submittedByPhone == workerPhone,
                                onConfirm = { viewModel.confirmAttendance(assignmentId, record.id) },
                                onDispute = { viewModel.disputeAttendance(assignmentId, record.id) }
                            )
                        }
                    }
                    // Submit form always visible so worker can log attendance for today
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("تسجيل حضور", style = MaterialTheme.typography.titleSmall)
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
                                ) { Text("تسجيل الحضور ليوم اليوم") }
                            }
                        }
                    }
                }
                is WorkerAttendanceUiState.Error -> item { ErrorMessage(message = att.message) }
            }

            item { HorizontalDivider() }

            // ── Completion ────────────────────────────────────────────────────
            item { Text("إتمام المهمة", style = MaterialTheme.typography.titleMedium) }

            when (val comp = completionState) {
                is WorkerCompletionUiState.Loading -> item { LoadingIndicator() }
                is WorkerCompletionUiState.NotSubmitted -> item {
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
                            ) { Text("تقديم إتمام المهمة") }
                        }
                    }
                }
                is WorkerCompletionUiState.Submitted -> item {
                    WorkerCompletionCard(
                        record = comp.record,
                        isLoading = isActionLoading,
                        isSelfSubmitted = comp.record.submittedByPhone == workerPhone,
                        onConfirm = { viewModel.confirmCompletion(assignmentId) },
                        onDispute = { viewModel.disputeCompletion(assignmentId) }
                    )
                }
                is WorkerCompletionUiState.Error -> item { ErrorMessage(message = comp.message) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WorkerAttendanceCard(
    record: AttendanceResponse,
    isLoading: Boolean,
    isSelfSubmitted: Boolean,
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
                text = "بواسطة: ${record.submittedByPhone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.status == "SUBMITTED") {
                if (isSelfSubmitted) {
                    val hours = hoursUntilAutoConfirm(record.submittedAt)
                    Text(
                        "يتأكد تلقائياً بعد $hours ساعة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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
}

@Composable
private fun WorkerCompletionCard(
    record: CompletionResponse,
    isLoading: Boolean,
    isSelfSubmitted: Boolean,
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
                text = "بواسطة: ${record.submittedByPhone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall)
            }
            if (record.status == "SUBMITTED") {
                if (isSelfSubmitted) {
                    val hours = hoursUntilAutoConfirm(record.submittedAt)
                    Text(
                        "يتأكد تلقائياً بعد $hours ساعة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
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
}

@Composable
private fun AttendanceBadge(status: String) {
    val (label, color) = when (status) {
        "SUBMITTED" -> "مقدّم"       to MaterialTheme.colorScheme.secondary
        "CONFIRMED" -> "مؤكد"        to MaterialTheme.colorScheme.tertiary
        "DISPUTED"  -> "متنازع عليه" to MaterialTheme.colorScheme.error
        "CANCELLED" -> "ملغى"        to MaterialTheme.colorScheme.error
        else        -> "حالة غير معروفة" to MaterialTheme.colorScheme.onSurfaceVariant
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
        else        -> "حالة غير معروفة" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
