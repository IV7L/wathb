package com.yawmiyati.app.ui.screens.business

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Work
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
import com.yawmiyati.app.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessMainScreen(
    onCreateJob: () -> Unit,
    onViewApplications: (jobId: String) -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit,
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val unreadCount by notificationsViewModel.unreadCount.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> BusinessHomeScreen(
                    onCreateJob        = onCreateJob,
                    onViewApplications = onViewApplications,
                    onWages            = { selectedTab = 1 },
                    onLogout           = onLogout,
                )
                1 -> WageObligationsScreen()
            }
        }

        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                icon     = { Icon(Icons.Filled.Work, contentDescription = "الوظائف") },
                label    = { Text("وظائفي") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                icon     = { Icon(Icons.Filled.Payments, contentDescription = "الأجور") },
                label    = { Text("الأجور") }
            )
            NavigationBarItem(
                selected = false,
                onClick  = onNotifications,
                icon = {
                    BadgedBox(badge = {
                        if (unreadCount > 0) Badge { Text(unreadCount.toString()) }
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات")
                    }
                },
                label = { Text("إشعارات") }
            )
        }
    }
}
