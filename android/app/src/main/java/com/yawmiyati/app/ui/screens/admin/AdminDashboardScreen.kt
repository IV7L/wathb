package com.yawmiyati.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.PilotSummaryResponse
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AdminDashboardViewModel
import com.yawmiyati.app.viewmodels.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("لوحة التحكم") }) }
    ) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> LoadingIndicator()
            is DashboardUiState.Error   -> ErrorMessage(
                message = state.message,
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
            is DashboardUiState.Success -> DashboardContent(
                summary = state.summary,
                onRefresh = { viewModel.load() },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    summary: PilotSummaryResponse,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionLabel("الحسابات الموثّقة")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("منشآت موثّقة", summary.verifiedBusinesses, Modifier.weight(1f))
            MetricCard("عمال موثّقون", summary.verifiedWorkers, Modifier.weight(1f))
        }

        SectionLabel("الوظائف والتقديمات")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("وظائف منشورة", summary.publishedJobs, Modifier.weight(1f))
            MetricCard("تقديمات", summary.applications, Modifier.weight(1f))
        }

        SectionLabel("المهام")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("مهام مقبولة", summary.acceptedAssignments, Modifier.weight(1f))
            MetricCard("أيام عمل مكتملة", summary.completedWorkerDays, Modifier.weight(1f))
        }

        SectionLabel("الأجور")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("التزامات مُنشأة", summary.wageObligationsCreated, Modifier.weight(1f))
            MetricCard("مدفوعات مُعلنة", summary.paymentsDeclared, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val totalIls = summary.totalVerifiedPaidMinor / 100.0
            MetricCard("مدفوعات موثّقة", summary.paymentsVerifiedPaid, Modifier.weight(1f))
            MetricCard("إجمالي موثّق (₪)", totalIls.toInt(), Modifier.weight(1f))
        }

        SectionLabel("النزاعات")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("نزاعات مفتوحة", summary.disputesOpened, Modifier.weight(1f))
            MetricCard("نزاعات محلولة", summary.disputesResolved, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text("تحديث")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MetricCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
