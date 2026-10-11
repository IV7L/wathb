package com.yawmiyati.app.ui.screens.admin

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
import com.yawmiyati.app.data.api.PaymentDeclarationResponse
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AdminWagesViewModel
import com.yawmiyati.app.viewmodels.WageActionState
import com.yawmiyati.app.viewmodels.WagesUiState
import com.yawmiyati.app.viewmodels.selectReviewableDeclaration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWagesScreen(
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: AdminWagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val sessionExpired by viewModel.sessionExpired.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    data class VerifyTarget(val obligation: WageObligationResponse, val declaration: PaymentDeclarationResponse, val isReject: Boolean)
    var verifyTarget by remember { mutableStateOf<VerifyTarget?>(null) }

    LaunchedEffect(sessionExpired) {
        if (sessionExpired) onNavigateToLogin()
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is WageActionState.Success -> {
                snackbarHostState.showSnackbar("تمت المعالجة بنجاح")
                verifyTarget = null
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
                title = { Text("مراجعة الدفعات") },
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
                onRefresh = { viewModel.loadPendingObligations() },
                modifier = Modifier.padding(padding)
            ) {
                EmptyState(message = "لا توجد دفعات بانتظار المراجعة", modifier = Modifier.fillMaxSize())
            }
            is WagesUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.loadPendingObligations() },
                modifier = Modifier.padding(padding)
            )
            is WagesUiState.Success -> PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { viewModel.loadPendingObligations() },
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
                        AdminWageCard(
                            obligation = obligation,
                            isActionLoading = actionState is WageActionState.Loading,
                            onVerify = { decl ->
                                verifyTarget = VerifyTarget(obligation, decl, isReject = false)
                            },
                            onReject = { decl ->
                                verifyTarget = VerifyTarget(obligation, decl, isReject = true)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    verifyTarget?.let { target ->
        VerifyDeclarationDialog(
            obligation = target.obligation,
            declaration = target.declaration,
            isReject = target.isReject,
            isLoading = actionState is WageActionState.Loading,
            onDismiss = { verifyTarget = null },
            onConfirm = { notes ->
                if (target.isReject) {
                    viewModel.rejectDeclaration(target.obligation.id, target.declaration.id, notes)
                } else {
                    viewModel.verifyDeclaration(target.obligation.id, target.declaration.id, notes)
                }
            }
        )
    }
}

@Composable
private fun AdminWageCard(
    obligation: WageObligationResponse,
    isActionLoading: Boolean,
    onVerify: (PaymentDeclarationResponse) -> Unit,
    onReject: (PaymentDeclarationResponse) -> Unit
) {
    // Declaration the admin must act on: the most recent SUBMITTED one.
    // Declarations arrive newest-first from the API, so firstOrNull by status is correct
    // even after a rejection + re-declaration cycle where both entries are present.
    val reviewableDecl = selectReviewableDeclaration(obligation.declarations)
    // For display, prefer the actionable declaration; fall back to the most recent one.
    val displayDecl = reviewableDecl ?: obligation.declarations.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(obligation.jobTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                AdminWageStatusBadge(obligation.status)
            }
            Text("العامل: ${obligation.workerEmail}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(obligation.amountDisplay, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary)

            if (displayDecl != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("تفاصيل الدفعة المُعلَنة", style = MaterialTheme.typography.labelMedium)
                Text("المبلغ: ${displayDecl.declaredAmount / 100} ${displayDecl.currency}",
                    style = MaterialTheme.typography.bodySmall)
                Text("الطريقة: ${paymentMethodLabel(displayDecl.paymentMethod)}",
                    style = MaterialTheme.typography.bodySmall)
                Text("التاريخ: ${displayDecl.paymentDate}", style = MaterialTheme.typography.bodySmall)
                if (displayDecl.transactionReference.isNotBlank()) {
                    Text("المرجع: ${displayDecl.transactionReference}", style = MaterialTheme.typography.bodySmall)
                }
                Text("بواسطة: ${displayDecl.declaredByEmail}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (reviewableDecl != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onVerify(reviewableDecl) },
                            enabled = !isActionLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تأكيد الدفع")
                        }
                        OutlinedButton(
                            onClick = { onReject(reviewableDecl) },
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
            } else {
                Text("لا توجد تفاصيل دفعة بعد", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VerifyDeclarationDialog(
    obligation: WageObligationResponse,
    declaration: PaymentDeclarationResponse,
    isReject: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var notes by rememberSaveable { mutableStateOf("") }
    val title = if (isReject) "رفض الدفعة" else "تأكيد الدفعة"
    val confirmLabel = if (isReject) "رفض" else "تأكيد"
    val canConfirm = !isReject || notes.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${obligation.jobTitle} — ${obligation.amountDisplay}")
                Text("طريقة الدفع: ${paymentMethodLabel(declaration.paymentMethod)}")
                if (isReject) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("سبب الرفض (مطلوب)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes) },
                enabled = !isLoading && canConfirm,
                colors = if (isReject) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("إلغاء") }
        }
    )
}

@Composable
private fun AdminWageStatusBadge(status: String) {
    val (label, color) = when (status) {
        "AWAITING_ACK"  -> "بانتظار التأكيد" to MaterialTheme.colorScheme.primary
        "UNDER_REVIEW"  -> "قيد المراجعة"    to MaterialTheme.colorScheme.secondary
        else            -> status              to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

private fun paymentMethodLabel(method: String): String = when (method) {
    "CASH"          -> "نقداً"
    "BANK_TRANSFER" -> "تحويل بنكي"
    else            -> method
}
