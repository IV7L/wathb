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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.WageObligationResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.WageActionState
import com.yawmiyati.app.viewmodels.WagesUiState
import com.yawmiyati.app.viewmodels.WagesViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WageObligationsScreen(
    onBack: () -> Unit,
    viewModel: WagesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var declareDialogObligation by remember { mutableStateOf<WageObligationResponse?>(null) }

    LaunchedEffect(actionState) {
        when (actionState) {
            is WageActionState.Success -> {
                snackbarHostState.showSnackbar("تم تسجيل الدفع بنجاح")
                declareDialogObligation = null
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
                title = { Text("الالتزامات المالية") },
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
                EmptyState(message = "لا توجد التزامات مالية حالياً", modifier = Modifier.fillMaxSize())
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
                        WageObligationCard(
                            obligation = obligation,
                            onDeclare = { declareDialogObligation = obligation }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    declareDialogObligation?.let { obligation ->
        DeclarePaymentDialog(
            obligation = obligation,
            isLoading = actionState is WageActionState.Loading,
            onDismiss = { declareDialogObligation = null },
            onConfirm = { amount, method, date, ref ->
                viewModel.declarePayment(obligation.id, amount, date, method, ref)
            }
        )
    }
}

@Composable
private fun WageObligationCard(
    obligation: WageObligationResponse,
    onDeclare: () -> Unit
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
                text = "العامل: ${obligation.workerEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            if (obligation.status in listOf("PENDING", "DUE", "REJECTED")) {
                Button(
                    onClick = onDeclare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("الإعلان عن دفع الأجر")
                }
            }
        }
    }
}

@Composable
private fun DeclarePaymentDialog(
    obligation: WageObligationResponse,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, method: String, date: String, ref: String) -> Unit
) {
    val paymentMethods = listOf("CASH" to "نقداً", "BANK_TRANSFER" to "تحويل بنكي")
    var selectedMethod by rememberSaveable { mutableStateOf("CASH") }
    var transactionRef by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now().toString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإعلان عن دفع الأجر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "المبلغ: ${obligation.amountDisplay}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("طريقة الدفع:", style = MaterialTheme.typography.labelMedium)
                paymentMethods.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedMethod == value,
                            onClick = { selectedMethod = value }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (selectedMethod == "BANK_TRANSFER") {
                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text("رقم المرجع / التحويل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(obligation.amount, selectedMethod, today, transactionRef)
                },
                enabled = !isLoading && (selectedMethod == "CASH" || transactionRef.isNotBlank())
            ) {
                Text("تأكيد")
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
        "DECLARED"      -> "تم الإعلان"        to MaterialTheme.colorScheme.primary
        "AWAITING_ACK"  -> "بانتظار التأكيد"   to MaterialTheme.colorScheme.primary
        "UNDER_REVIEW"  -> "قيد المراجعة"      to MaterialTheme.colorScheme.secondary
        "VERIFIED_PAID" -> "مدفوع ومؤكد"       to MaterialTheme.colorScheme.tertiary
        "REJECTED"      -> "مرفوض"             to MaterialTheme.colorScheme.error
        "DISPUTED"      -> "متنازع عليه"       to MaterialTheme.colorScheme.error
        "CANCELLED"     -> "ملغى"              to MaterialTheme.colorScheme.error
        else            -> status               to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
