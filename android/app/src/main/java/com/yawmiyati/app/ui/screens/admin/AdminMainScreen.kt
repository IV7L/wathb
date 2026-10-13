package com.yawmiyati.app.ui.screens.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AdminDashboardScreen()
                1 -> AdminVerificationScreen(onLogout = onLogout)
                2 -> AdminWorkersScreen()
                3 -> AdminDisputesScreen()
            }
        }

        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick  = { selectedTab = 0 },
                icon     = { Icon(Icons.Filled.Dashboard, contentDescription = "اللوحة") },
                label    = { Text("اللوحة") }
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick  = { selectedTab = 1 },
                icon     = { Icon(Icons.Filled.Business, contentDescription = "المنشآت") },
                label    = { Text("المنشآت") }
            )
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick  = { selectedTab = 2 },
                icon     = { Icon(Icons.Filled.Group, contentDescription = "العمال") },
                label    = { Text("العمال") }
            )
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick  = { selectedTab = 3 },
                icon     = { Icon(Icons.Filled.Gavel, contentDescription = "الاعتراضات") },
                label    = { Text("الاعتراضات") }
            )
        }
    }
}
