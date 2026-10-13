package com.yawmiyati.app.ui.screens.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmiyati.app.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerMainScreen(
    onJobClick: (jobId: String) -> Unit,
    onManageAssignment: (assignmentId: String, workerPhone: String, businessPhone: String, assignmentStatus: String) -> Unit,
    onLogout: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

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
                    onNotifications  = onOpenNotifications,
                )
                1 -> WorkerMyWorkScreen(onManageAssignment = onManageAssignment)
                2 -> WorkerMapScreen(onJobClick = onJobClick)
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
                icon     = { Icon(Icons.Filled.Map, contentDescription = "الخريطة") },
                label    = { Text("الخريطة") }
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
