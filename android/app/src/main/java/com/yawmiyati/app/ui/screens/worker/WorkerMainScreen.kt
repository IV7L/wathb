package com.yawmiyati.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.viewmodels.NotificationsViewModel

private data class WorkerTab(val label: String, val icon: ImageVector)

private val WORKER_TABS = listOf(
    WorkerTab("وظائف",   Icons.Filled.Work),
    WorkerTab("طلباتي",  Icons.AutoMirrored.Filled.Assignment),
    WorkerTab("مهامي",   Icons.Filled.DateRange),
    WorkerTab("أجوري",   Icons.Filled.Payments),
    WorkerTab("ملفي",    Icons.Filled.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMainScreen(
    onJobClick: (jobId: String) -> Unit,
    onManageAssignment: (assignmentId: String, workerPhone: String, businessPhone: String) -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit,
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    // Column + weight so the inner Scaffold fills the correct space
    // and NavigationBar sits below — no nested Scaffold overlap issues.
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WorkerHomeScreen(
                    onJobClick       = onJobClick,
                    onMyApplications = { selectedTab = 1 },
                    onMyAssignments  = { selectedTab = 2 },
                    onMyWages        = { selectedTab = 3 },
                    onProfile        = { selectedTab = 4 },
                    onLogout         = onLogout,
                    onNotifications  = onNotifications,
                    unreadCount      = unreadCount,
                )
                1 -> WorkerApplicationsScreen()
                2 -> WorkerAssignmentsScreen(onManageAssignment = onManageAssignment)
                3 -> WorkerWagesScreen()
                4 -> WorkerProfileScreen()
            }
        }

        NavigationBar {
            WORKER_TABS.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected  = selectedTab == index,
                    onClick   = { selectedTab = index },
                    icon      = { Icon(tab.icon, contentDescription = tab.label) },
                    label     = { Text(tab.label) }
                )
            }
        }
    }
}
