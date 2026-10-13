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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.yawmiyati.app.ui.theme.WathbDeepTeal
import com.yawmiyati.app.ui.theme.WathbGold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.ui.components.ConfirmCancelDialog
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.ui.components.SupportWhatsappButton
import com.yawmiyati.app.viewmodels.AuthViewModel
import com.yawmiyati.app.viewmodels.CancelJobUiState
import com.yawmiyati.app.viewmodels.CloneJobUiState
import com.yawmiyati.app.viewmodels.JobsUiState
import com.yawmiyati.app.viewmodels.JobsViewModel
import com.yawmiyati.app.viewmodels.PublishUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHomeScreen(
    onCreateJob: () -> Unit,
    onViewApplications: (jobId: String) -> Unit,
    onWages: () -> Unit,
    onProfile: () -> Unit = {},
    onLogout: () -> Unit,
    onNotifications: () -> Unit = {},
    unreadCount: Int = 0,
    jobsViewModel: JobsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val jobsState by jobsViewModel.uiState.collectAsState()
    val publishState by jobsViewModel.publishState.collectAsState()
    val cancelState by jobsViewModel.cancelState.collectAsState()
    val cloneState by jobsViewModel.cloneState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var cancelJobId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(publishState) {
        if (publishState is PublishUiState.Error) {
            snackbarHostState.showSnackbar((publishState as PublishUiState.Error).message)
            jobsViewModel.resetPublishError()
        }
    }

    LaunchedEffect(cancelState) {
        when (cancelState) {
            is CancelJobUiState.Cancelled -> {
                cancelJobId = null
                snackbarHostState.showSnackbar("تم إلغاء الوظيفة")
                jobsViewModel.resetCancelState()
            }
            is CancelJobUiState.Error -> {
                snackbarHostState.showSnackbar((cancelState as CancelJobUiState.Error).message)
                jobsViewModel.resetCancelState()
            }
            else -> {}
        }
    }

    LaunchedEffect(cloneState) {
        when (cloneState) {
            is CloneJobUiState.Cloned -> {
                snackbarHostState.showSnackbar("تم إنشاء مسودة جديدة — ابحث عنها في قائمة الوظائف")
                jobsViewModel.resetCloneState()
            }
            is CloneJobUiState.Error -> {
                snackbarHostState.showSnackbar((cloneState as CloneJobUiState.Error).message)
                jobsViewModel.resetCloneState()
            }
            else -> {}
        }
    }

    cancelJobId?.let { jobId ->
        ConfirmCancelDialog(
            title = "إلغاء الوظيفة",
            body = "سيتم إلغاء جميع الطلبات والمهام المرتبطة بهذه الوظيفة.",
            isLoading = cancelState is CancelJobUiState.Loading,
            onConfirm = { reason -> jobsViewModel.cancelJob(jobId, reason) },
            onDismiss = { cancelJobId = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("وَثْب", style = MaterialTheme.typography.titleLarge, color = WathbGold)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WathbDeepTeal,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = {
                            if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "الملف الشخصي", tint = Color.White)
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateJob,
                containerColor = WathbDeepTeal
            ) {
                Icon(Icons.Filled.Add, contentDescription = "نشر وظيفة", tint = Color.White)
            }
        }
    ) { padding ->
        when (val state = jobsState) {
            is JobsUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))
            is JobsUiState.Empty -> PullToRefreshBox(
                isRefreshing = false,
                onRefresh = { jobsViewModel.refresh() },
                modifier = Modifier.padding(padding)
            ) {
                EmptyState(
                    message = "لا توجد وظائف منشورة بعد.\nاضغط + لنشر وظيفتك الأولى.",
                    modifier = Modifier.fillMaxSize()
                )
            }
            is JobsUiState.Error -> {
                if (state.message.contains("التحقق") || state.message.contains("مصرح") || state.isAuth) {
                    // Unverified business — show informational message
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "حسابك في انتظار التحقق",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "يتم التحقق من الحسابات التجارية من قِبَل فريق وَثْب قبل السماح بنشر الوظائف. سنُخطرك عند الموافقة.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { jobsViewModel.loadJobs() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is JobsUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { jobsViewModel.refresh() },
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
                        items(state.jobs) { job ->
                            BusinessJobCard(
                                job = job,
                                isPublishing = publishState is PublishUiState.Loading &&
                                    (publishState as PublishUiState.Loading).jobId == job.id,
                                isCloning = cloneState is CloneJobUiState.Cloning,
                                onClick = { onViewApplications(job.id) },
                                onPublish = { jobsViewModel.publishJob(job.id) },
                                onCancel = { cancelJobId = job.id },
                                onClone = { jobsViewModel.cloneJob(job.id) }
                            )
                        }
                        item {
                            SupportWhatsappButton(modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessJobCard(
    job: JobListingResponse,
    isPublishing: Boolean,
    isCloning: Boolean = false,
    onClick: () -> Unit,
    onPublish: () -> Unit,
    onCancel: () -> Unit,
    onClone: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = job.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${job.locationCity} · ${job.workDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = job.wageDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (job.status == "DRAFT") {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onPublish,
                    enabled = !isPublishing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("نشر الوظيفة")
                }
            }
            if (job.status == "PUBLISHED") {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error
                    )
                ) { Text("إلغاء الوظيفة") }
            }
            if (job.status in listOf("FILLED", "CANCELLED")) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClone,
                    enabled = !isCloning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isCloning) "جارٍ النسخ..." else "انشر مرة ثانية") }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "PUBLISHED"      -> "منشور" to MaterialTheme.colorScheme.primary
        "DRAFT"          -> "مسودة" to MaterialTheme.colorScheme.onSurfaceVariant
        "PENDING_REVIEW" -> "قيد المراجعة" to MaterialTheme.colorScheme.secondary
        "FILLED"         -> "مكتمل" to MaterialTheme.colorScheme.tertiary
        "CANCELLED"      -> "ملغى" to MaterialTheme.colorScheme.error
        else             -> "حالة غير معروفة" to MaterialTheme.colorScheme.onSurfaceVariant
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
