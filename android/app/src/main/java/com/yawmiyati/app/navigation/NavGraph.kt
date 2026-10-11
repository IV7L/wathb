package com.yawmiyati.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yawmiyati.app.YawmiyatiApp
import com.yawmiyati.app.data.auth.SessionEventBus
import com.yawmiyati.app.ui.screens.admin.AdminVerificationScreen
import com.yawmiyati.app.ui.screens.admin.AdminWagesScreen
import com.yawmiyati.app.ui.screens.auth.LoginScreen
import com.yawmiyati.app.ui.screens.auth.RegisterScreen
import com.yawmiyati.app.ui.screens.business.BusinessAssignmentDetailScreen
import com.yawmiyati.app.ui.screens.business.BusinessHomeScreen
import com.yawmiyati.app.ui.screens.business.JobApplicationsScreen
import com.yawmiyati.app.ui.screens.business.JobCreateScreen
import com.yawmiyati.app.ui.screens.business.WageObligationsScreen
import com.yawmiyati.app.ui.screens.worker.JobDetailScreen
import com.yawmiyati.app.ui.screens.worker.WorkerAssignmentDetailScreen
import com.yawmiyati.app.ui.screens.worker.WorkerAssignmentsScreen
import com.yawmiyati.app.ui.screens.worker.WorkerHomeScreen
import com.yawmiyati.app.ui.screens.worker.WorkerWagesScreen

@Composable
fun YawmiyatiNavGraph() {
    val navController = rememberNavController()
    val tokenManager = YawmiyatiApp.instance.tokenManager

    val startDestination = if (tokenManager.isLoggedIn()) {
        when (tokenManager.getRole()) {
            "WORKER"   -> Screen.WorkerHome.route
            "BUSINESS" -> Screen.BusinessHome.route
            "ADMIN"    -> Screen.AdminVerification.route
            else       -> Screen.Login.route
        }
    } else {
        Screen.Login.route
    }

    LaunchedEffect(Unit) {
        SessionEventBus.sessionExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = when (role) {
                        "WORKER"   -> Screen.WorkerHome.route
                        "BUSINESS" -> Screen.BusinessHome.route
                        "ADMIN"    -> Screen.AdminVerification.route
                        else       -> Screen.Login.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    val dest = when (role) {
                        "WORKER"   -> Screen.WorkerHome.route
                        "BUSINESS" -> Screen.BusinessHome.route
                        "ADMIN"    -> Screen.AdminVerification.route
                        else       -> Screen.Login.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkerHome.route) {
            WorkerHomeScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onMyAssignments = {
                    navController.navigate(Screen.WorkerAssignments.route)
                },
                onMyWages = {
                    navController.navigate(Screen.WorkerWages.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.WorkerAssignments.route) {
            WorkerAssignmentsScreen(
                onBack = { navController.popBackStack() },
                onManageAssignment = { assignmentId ->
                    navController.navigate(Screen.WorkerAssignmentDetail.createRoute(assignmentId))
                }
            )
        }

        composable(
            route = Screen.WorkerAssignmentDetail.route,
            arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: return@composable
            WorkerAssignmentDetailScreen(
                assignmentId = assignmentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WorkerWages.route) {
            WorkerWagesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            JobDetailScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() },
                onSessionExpired = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.BusinessHome.route) {
            BusinessHomeScreen(
                onCreateJob = { navController.navigate(Screen.JobCreate.route) },
                onViewApplications = { jobId ->
                    navController.navigate(Screen.JobApplications.createRoute(jobId))
                },
                onWages = { navController.navigate(Screen.BusinessWages.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.JobCreate.route) {
            JobCreateScreen(
                onJobCreated = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BusinessWages.route) {
            WageObligationsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BusinessAssignment.route,
            arguments = listOf(
                navArgument("assignmentId") { type = NavType.StringType },
                navArgument("workerEmail") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: return@composable
            val workerEmail = backStackEntry.arguments?.getString("workerEmail") ?: ""
            BusinessAssignmentDetailScreen(
                assignmentId = assignmentId,
                workerEmail = workerEmail,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JobApplications.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            JobApplicationsScreen(
                jobId = jobId,
                onBack = { navController.popBackStack() },
                onManageAssignment = { assignmentId, workerEmail ->
                    navController.navigate(Screen.BusinessAssignment.createRoute(assignmentId, workerEmail))
                }
            )
        }

        composable(Screen.AdminVerification.route) {
            AdminVerificationScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onWageReview = { navController.navigate(Screen.AdminWages.route) }
            )
        }

        composable(Screen.AdminWages.route) {
            AdminWagesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
