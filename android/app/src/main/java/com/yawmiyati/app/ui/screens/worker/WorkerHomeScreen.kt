package com.yawmiyati.app.ui.screens.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.graphics.Color
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
import com.yawmiyati.app.ui.components.SupportWhatsappButton
import com.yawmiyati.app.ui.theme.WathbDeepTeal
import com.yawmiyati.app.ui.theme.WathbGold
import com.yawmiyati.app.ui.utils.formatArabicDate
import com.yawmiyati.app.ui.utils.formatArabicWage
import com.yawmiyati.app.viewmodels.JobsUiState
import com.yawmiyati.app.viewmodels.JobsViewModel
import com.yawmiyati.app.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHomeScreen(
    onJobClick: (jobId: String) -> Unit,
    onMyApplications: () -> Unit,
    onMyAssignments: () -> Unit,
    onMyWages: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
    onNotifications: () -> Unit = {},
    jobsViewModel: JobsViewModel = viewModel(),
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    val jobsState by jobsViewModel.uiState.collectAsState()
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()
    val isRefreshing = (jobsState as? JobsUiState.Success)?.isRefreshing == true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "وَثْب",
                        style = MaterialTheme.typography.titleLarge,
                        color = WathbGold
                    )
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
                        item {
                            SupportWhatsappButton(modifier = Modifier.padding(horizontal = 4.dp))
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
            // Header row: category circle + title/business + جديد badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                CategoryCircle(category = job.category)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = job.businessName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (job.isNew) {
                    Surface(
                        color = WathbGold,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "جديد",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Wage — largest element
            Text(
                text = formatArabicWage(job.wageAmount, job.wageCurrency, job.wageBasis),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            // Location + date row
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
                    text = formatArabicDate(job.workDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Track record
            val trackLabel = job.businessTrackRecord?.label
            if (!trackLabel.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
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

@Composable
private fun CategoryCircle(category: String?) {
    val (emoji, bgColor) = categoryVisual(category)
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(bgColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
    }
}

private fun categoryVisual(category: String?): Pair<String, Color> = when (category) {
    "LOADING"      -> "📦" to Color(0xFFB3E5FC)
    "CLEANING"     -> "🧹" to Color(0xFFB2EBF2)
    "CONSTRUCTION" -> "🏗️" to Color(0xFFFFCCBC)
    "AGRICULTURE"  -> "🌿" to Color(0xFFC8E6C9)
    "DELIVERY"     -> "🚚" to Color(0xFFFFE0B2)
    "FOOD"         -> "🍽️" to Color(0xFFF8BBD9)
    "RETAIL"       -> "🛒" to Color(0xFFE1BEE7)
    else           -> "💼" to Color(0xFFE0E0E0)
}

