package com.yawmiyati.app.ui.screens.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.data.api.JobListingResponse
import com.yawmiyati.app.ui.components.EmptyState
import com.yawmiyati.app.ui.components.ErrorMessage
import com.yawmiyati.app.ui.components.LoadingIndicator
import com.yawmiyati.app.viewmodels.AuthViewModel
import com.yawmiyati.app.viewmodels.JobsUiState
import com.yawmiyati.app.viewmodels.JobsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHomeScreen(
    onJobClick: (jobId: String) -> Unit,
    onMyApplications: () -> Unit,
    onMyAssignments: () -> Unit,
    onMyWages: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    jobsViewModel: JobsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val jobsState by jobsViewModel.uiState.collectAsState()
    val isRefreshing = (jobsState as? JobsUiState.Success)?.isRefreshing == true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("الوظائف المتاحة", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onMyApplications) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "طلباتي")
                    }
                    IconButton(onClick = onMyAssignments) {
                        Icon(Icons.Filled.DateRange, contentDescription = "مهامي")
                    }
                    IconButton(onClick = onMyWages) {
                        Icon(Icons.Filled.Payments, contentDescription = "أجوري")
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "ملفي")
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل الخروج")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = jobsState) {
            is JobsUiState.Loading -> LoadingIndicator(modifier = Modifier.padding(padding))

            is JobsUiState.Empty -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { jobsViewModel.refresh() },
                modifier = Modifier.padding(padding)
            ) {
                EmptyState(
                    message = "لا توجد وظائف متاحة حالياً",
                    modifier = Modifier.fillMaxSize()
                )
            }

            is JobsUiState.Error -> ErrorMessage(
                message = state.message,
                onRetry = { jobsViewModel.loadJobs() },
                modifier = Modifier.padding(padding)
            )

            is JobsUiState.Success -> {
                val listState = rememberLazyListState()

                // Trigger next page when last visible item is near the end
                val loadMore by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val totalItems = info.totalItemsCount
                        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                        totalItems > 0 && lastVisible >= totalItems - 3 && state.hasMore
                    }
                }
                LaunchedEffect(loadMore) {
                    if (loadMore) jobsViewModel.loadNextPage()
                }

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { jobsViewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(state.jobs) { job ->
                            JobCard(job = job, onClick = { onJobClick(job.id) })
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobCard(job: JobListingResponse, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = job.businessName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = job.locationCity,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = job.workDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = job.wageDisplay,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            val trackLabel = job.businessTrackRecord?.label
            if (!trackLabel.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = trackLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (job.isFunded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ممول",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
