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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.WageActionState
import com.yawmiyati.app.viewmodels.WagesUiState
import com.yawmiyati.app.viewmodels.WorkerWagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerWagesScreen(
    onBack: () -> Unit,
    viewModel: WorkerWagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmObligation by remember { mutableStateOf<WageObligationResponse?>(null) }
    var disputeObligation by remember { mutableStateOf<WageObligationResponse?>(null) }

    LaunchedEffect(actionState) {
        when (actionState) {
            is WageActionState.Success -> {
                snackbarHostState.showSnackbar("تم الإجراء بنجاح")
                viewModel.resetActionState()
            }
            is WageActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as WageActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أجوري") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is WagesUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is WagesUiState.Empty   -> PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { viewModel.loadObligations() },
                modifier = Modifier.padding(padding)
            ) {
                EmptyState(message = "لا توجد التزامات أجور حالياً", modifier = Modifier.fillMaxSize())
            }
            is WagesUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.loadObligations() },
                modifier = Modifier.padding(padding)
            )
            is WagesUiState.Success -> PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { viewModel.loadObligations() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(state.obligations, key = { it.id }) { obligation ->
                        WorkerWageCard(
                            obligation = obligation,
                            isActionLoading = actionState is WageActionState.Loading,
                            onAcknowledge = { confirmObligation = obligation },
                            onDispute = { disputeObligation = obligation }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    confirmObligation?.let { obligation ->
        AcknowledgeConfirmDialog(
            obligation = obligation,
            isLoading = actionState is WageActionState.Loading,
            onDismiss = { confirmObligation = null },
            onConfirm = {
                viewModel.acknowledgePayment(obligation.id)
                confirmObligation = null
            }
        )
    }

    disputeObligation?.let { obligation ->
        DisputePaymentDialog(
            obligation = obligation,
            isLoading = actionState is WageActionState.Loading,
            onDismiss = { disputeObligation = null },
            onConfirm = { reason ->
                viewModel.disputePayment(obligation.id, reason)
                disputeObligation = null
            }
        )
    }
}

@Composable
private fun WorkerWageCard(
    obligation: WageObligationResponse,
    isActionLoading: Boolean,
    onAcknowledge: () -> Unit,
    onDispute: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = obligation.jobTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                WageStatusBadge(obligation.status)
            }
            Text(
                text = obligation.amountDisplay,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "تاريخ الاستحقاق: ${obligation.dueDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val latestDecl = obligation.declarations.lastOrNull()
            if (latestDecl != null) {
                Text(
                    text = "طريقة الدفع: ${paymentMethodLabel(latestDecl.paymentMethod)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "تاريخ الدفع المُعلَن: ${latestDecl.paymentDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (obligation.status == "AWAITING_ACK") {
                Button(
                    onClick = onAcknowledge,
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تأكيد استلام الأجر")
                }
                OutlinedButton(
                    onClick = onDispute,
                    enabled = !isActionLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("لم أستلم أجري")
                }
            }
        }
    }
}

@Composable
private fun AcknowledgeConfirmDialog(
    obligation: WageObligationResponse,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد استلام الأجر") },
        text = {
            Text(
                "هل تؤكد استلام ${obligation.amountDisplay} نقداً من صاحب العمل؟\n\n" +
                "لا يمكن التراجع عن هذا الإجراء.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                Text("نعم، استلمت الأجر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("إلغاء") }
        }
    )
}

@Composable
private fun DisputePaymentDialog(
    obligation: WageObligationResponse,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإبلاغ عن عدم استلام الأجر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "يرجى وصف المشكلة المتعلقة بأجر ${obligation.amountDisplay}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الاعتراض *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    isError = reason.isBlank()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = !isLoading && reason.isNotBlank(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("إرسال الاعتراض")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("إلغاء") }
        }
    )
}

@Composable
private fun WageStatusBadge(status: String) {
    val (label, color) = when (status) {
        "PENDING"       -> "معلق"              to MaterialTheme.colorScheme.secondary
        "DUE"           -> "مستحق"             to MaterialTheme.colorScheme.error
        "AWAITING_ACK"  -> "بانتظار تأكيدك"   to MaterialTheme.colorScheme.primary
        "UNDER_REVIEW"  -> "قيد المراجعة"      to MaterialTheme.colorScheme.secondary
        "VERIFIED_PAID" -> "مدفوع ومؤكد"       to MaterialTheme.colorScheme.tertiary
        "REJECTED"      -> "مرفوض"             to MaterialTheme.colorScheme.error
        "DISPUTED"      -> "متنازع عليه"       to MaterialTheme.colorScheme.error
        else            -> status               to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

private fun paymentMethodLabel(method: String): String = when (method) {
    "CASH"          -> "نقداً"
    "BANK_TRANSFER" -> "تحويل بنكي"
    "OTHER"         -> "طريقة أخرى"
    else            -> method
}
