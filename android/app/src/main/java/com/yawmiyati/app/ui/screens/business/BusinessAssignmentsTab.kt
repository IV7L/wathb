package com.yawmiyati.app.ui.screens.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.AssignmentResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.ui.utils.formatArabicDate
import com.yawmiyati.app.viewmodels.AssignmentActionState
import com.yawmiyati.app.viewmodels.AssignmentsUiState
import com.yawmiyati.app.viewmodels.AssignmentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAssignmentsTab(
    onManageAssignment: (assignmentId: String, workerPhone: String, assignmentStatus: String) -> Unit = { _, _, _ -> },
    viewModel: AssignmentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (actionState) {
            is AssignmentActionState.Success -> {
                snackbarHostState.showSnackbar("تم بنجاح")
                viewModel.resetActionState()
            }
            is AssignmentActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as AssignmentActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val state = uiState) {
            is AssignmentsUiState.Loading -> LoadingIndicator(modifier = Modifier.fillMaxSize().padding(padding))
            is AssignmentsUiState.Empty   -> EmptyState(
                message = "لا توجد مهام نشطة حالياً",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is AssignmentsUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.loadAssignments() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is AssignmentsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.assignments, key = { it.id }) { assignment ->
                        BusinessAssignmentCard(
                            assignment = assignment,
                            isActionLoading = actionState is AssignmentActionState.Loading,
                            onComplete = { viewModel.businessComplete(assignment.id) },
                            onManage = { onManageAssignment(assignment.id, assignment.workerPhone, assignment.status) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BusinessAssignmentCard(
    assignment: AssignmentResponse,
    isActionLoading: Boolean,
    onComplete: () -> Unit = {},
    onManage: () -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = assignment.jobTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AssignmentStatusBadge(assignment.status)
            }

            Text(
                text = "العامل: ${assignment.workerPhone}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!assignment.jobWorkDate.isNullOrBlank()) {
                Text(
                    text = "يوم العمل: ${formatArabicDate(assignment.jobWorkDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (assignment.status) {
                "ACCEPTED", "ACTIVE" -> {
                    Button(
                        onClick = onComplete,
                        enabled = !isActionLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("تأكيد إتمام العمل")
                        }
                    }
                }
                "COMPLETED" -> {
                    Text(
                        "✓ بانتظار إقرار العامل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentStatusBadge(status: String) {
    val (label, color) = when (status) {
        "OFFERED"   -> "عرض جديد"   to MaterialTheme.colorScheme.primary
        "ACCEPTED"  -> "مقبول"       to MaterialTheme.colorScheme.tertiary
        "DECLINED"  -> "مرفوض"       to MaterialTheme.colorScheme.error
        "ACTIVE"    -> "نشط"         to MaterialTheme.colorScheme.secondary
        "COMPLETED" -> "مكتمل"       to MaterialTheme.colorScheme.tertiary
        "CANCELLED" -> "ملغى"        to MaterialTheme.colorScheme.error
        else        -> status        to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
