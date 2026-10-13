package com.yawmiyati.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.ui.screens.NotificationsScreen
import com.yawmiyati.app.viewmodels.AuthViewModel
import com.yawmiyati.app.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMainScreen(
    onJobClick: (jobId: String) -> Unit,
    onManageAssignment: (assignmentId: String, workerPhone: String, businessPhone: String, assignmentStatus: String) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WorkerHomeScreen(
                    onJobClick       = onJobClick,
                    onMyApplications = { selectedTab = 1 },
                    onMyAssignments  = { selectedTab = 1 },
                    onMyWages        = { selectedTab = 1 },
                    onProfile        = { selectedTab = 3 },
                    onLogout         = {},
                    onNotifications  = { selectedTab = 2 },
                    unreadCount      = unreadCount,
                )
                1 -> WorkerMyWorkScreen(onManageAssignment = onManageAssignment)
                2 -> NotificationsScreen(onBack = {})
                3 -> WorkerProfileScreen(
                    onLockApp = {
                        authViewModel.lockApp()
                        onLogout()
                    },
                    onLogout = {
                        authViewModel.logout()
                        onLogout()
                    }
                )
            }
        }

        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                icon     = { Icon(Icons.Filled.Home, contentDescription = "الرئيسية") },
                label    = { Text("الرئيسية") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                icon     = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "شغلي") },
                label    = { Text("شغلي") }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick  = { selectedTab = 2 },
                icon = {
                    BadgedBox(badge = {
                        if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات")
                    }
                },
                label = { Text("الإشعارات") }
            )
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick  = { selectedTab = 3 },
                icon     = { Icon(Icons.Filled.Person, contentDescription = "حسابي") },
                label    = { Text("حسابي") }
            )
        }
    }
}
