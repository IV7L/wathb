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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.AssignmentResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.ui.utils.formatArabicDate
import com.yawmiyati.app.ui.utils.formatArabicTime
import com.yawmiyati.app.ui.utils.formatArabicWage
import com.yawmiyati.app.ui.utils.toArabic
import com.yawmiyati.app.viewmodels.AssignmentActionState
import com.yawmiyati.app.viewmodels.AssignmentsUiState
import com.yawmiyati.app.viewmodels.AssignmentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerAssignmentsScreen(
    onBack: (() -> Unit)? = null,
    onManageAssignment: (assignmentId: String, workerPhone: String, businessPhone: String, assignmentStatus: String) -> Unit = { _, _, _, _ -> },
    viewModel: AssignmentsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        if (actionState is AssignmentActionState.Error) {
            snackbarHostState.showSnackbar((actionState as AssignmentActionState.Error).message)
            viewModel.resetActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مهامي") },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is AssignmentsUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is AssignmentsUiState.Empty   -> EmptyState(
                message = "لا توجد مهام حالياً",
                modifier = Modifier.padding(padding).fillMaxSize()
            )
            is AssignmentsUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.loadAssignments() },
                modifier = Modifier.padding(padding)
            )
            is AssignmentsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.assignments, key = { it.id }) { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            isActionLoading = actionState is AssignmentActionState.Loading,
                            onAccept  = { viewModel.acceptAssignment(assignment.id) },
                            onDecline = { viewModel.declineAssignment(assignment.id) },
                            onManage  = { onManageAssignment(assignment.id, assignment.workerPhone, assignment.businessPhone ?: "", assignment.status) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    assignment: AssignmentResponse,
    isActionLoading: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
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
                text = assignment.businessName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!assignment.offeredAt.isNullOrBlank()) {
                Text(
                    text = "تاريخ العرض: ${formatArabicDate(assignment.offeredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!assignment.acceptedAt.isNullOrBlank()) {
                Text(
                    text = "تاريخ القبول: ${formatArabicDate(assignment.acceptedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Show job terms on OFFERED cards so worker can review before accepting
            if (assignment.status == "OFFERED") {
                if (!assignment.jobWorkDate.isNullOrBlank()) {
                    Text(
                        text = "يوم العمل: ${formatArabicDate(assignment.jobWorkDate)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!assignment.jobStartTime.isNullOrBlank() && !assignment.jobEndTime.isNullOrBlank()) {
                    Text(
                        text = "الوقت: ${formatArabicTime(assignment.jobStartTime)} — ${formatArabicTime(assignment.jobEndTime)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (!assignment.jobLocationCity.isNullOrBlank()) {
                    Text(
                        text = "الموقع: ${assignment.jobLocationCity}${if (!assignment.jobLocationAddress.isNullOrBlank()) "، ${assignment.jobLocationAddress}" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (assignment.jobWageAmount != null && !assignment.jobWageCurrency.isNullOrBlank()) {
                    val basisLabel = when (assignment.jobWageBasis) {
                        "PER_DAY"  -> "يومي"
                        "PER_HOUR" -> "بالساعة"
                        "PER_TASK" -> "بالمهمة"
                        else       -> assignment.jobWageBasis ?: ""
                    }
                    val methodLabel = when (assignment.jobPaymentMethod) {
                        "CASH"          -> "نقداً"
                        "BANK_TRANSFER" -> "تحويل بنكي"
                        "OTHER"         -> "طريقة أخرى"
                        else            -> assignment.jobPaymentMethod ?: ""
                    }
                    val wageStr = formatArabicWage(
                        assignment.jobWageAmount,
                        assignment.jobWageCurrency ?: "ILS",
                        assignment.jobWageBasis ?: ""
                    )
                    Text(
                        text = "الأجر: $wageStr ($basisLabel) — $methodLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (assignment.jobPaymentDeadlineDays != null) {
                    Text(
                        text = "موعد الدفع: خلال ${assignment.jobPaymentDeadlineDays.toArabic()} أيام من الانتهاء",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Show agreed terms after acceptance — read from snapshot, not live job fields
            val snap = assignment.termsSnapshot
            if (snap != null && assignment.status !in listOf("OFFERED", "DECLINED")) {
                val snapWageAmount = (snap["wage_amount"] as? Number)?.toLong()
                val snapCurrency = snap["wage_currency"] as? String
                val snapBasis = snap["wage_basis"] as? String
                val snapMethod = snap["payment_method"] as? String
                val snapDeadlineDays = (snap["payment_deadline_days"] as? Number)?.toInt()
                val snapWorkDate = snap["work_date"] as? String
                val snapStart = snap["start_time"] as? String
                val snapEnd = snap["end_time"] as? String
                val snapCity = snap["location_city"] as? String
                val snapAddress = snap["location_address"] as? String

                Text(
                    text = "الشروط المتفق عليها",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!snapWorkDate.isNullOrBlank())
                    Text("يوم العمل: ${formatArabicDate(snapWorkDate)}", style = MaterialTheme.typography.bodySmall)
                if (!snapStart.isNullOrBlank() && !snapEnd.isNullOrBlank())
                    Text("الوقت: ${formatArabicTime(snapStart)} — ${formatArabicTime(snapEnd)}", style = MaterialTheme.typography.bodySmall)
                if (!snapCity.isNullOrBlank())
                    Text("الموقع: $snapCity${if (!snapAddress.isNullOrBlank()) "، $snapAddress" else ""}", style = MaterialTheme.typography.bodySmall)
                if (snapWageAmount != null && !snapCurrency.isNullOrBlank()) {
                    val basisLabel = when (snapBasis) {
                        "PER_DAY"  -> "يومي"
                        "PER_HOUR" -> "بالساعة"
                        "PER_TASK" -> "بالمهمة"
                        else       -> snapBasis ?: ""
                    }
                    val methodLabel = when (snapMethod) {
                        "CASH"          -> "نقداً"
                        "BANK_TRANSFER" -> "تحويل بنكي"
                        "OTHER"         -> "طريقة أخرى"
                        else            -> snapMethod ?: ""
                    }
                    val wageStr = formatArabicWage(snapWageAmount, snapCurrency, snapBasis ?: "")
                    Text(
                        "الأجر: $wageStr ($basisLabel) — $methodLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (snapDeadlineDays != null)
                    Text("موعد الدفع: خلال ${snapDeadlineDays.toArabic()} أيام", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (assignment.status == "OFFERED") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAccept,
                        enabled = !isActionLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("قبول المهمة")
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        enabled = !isActionLoading,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("رفض")
                    }
                }
            }

            if (assignment.status in listOf("ACCEPTED", "ACTIVE")) {
                Button(
                    onClick = onManage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد الحضور والإتمام")
                }
            }
        }
    }
}

@Composable
private fun AssignmentStatusBadge(status: String) {
    val (label, color) = when (status) {
        "OFFERED"   -> "عرض جديد"    to MaterialTheme.colorScheme.primary
        "ACCEPTED"  -> "مقبول"        to MaterialTheme.colorScheme.tertiary
        "DECLINED"  -> "مرفوض"        to MaterialTheme.colorScheme.error
        "ACTIVE"    -> "نشط"          to MaterialTheme.colorScheme.secondary
        "COMPLETED" -> "مكتمل"        to MaterialTheme.colorScheme.tertiary
        "CANCELLED" -> "ملغى"         to MaterialTheme.colorScheme.error
        else        -> "حالة غير معروفة" to MaterialTheme.colorScheme.onSurfaceVariant
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
