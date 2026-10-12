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
import com.yawmiyati.app.ui.screens.NotificationsScreen
import com.yawmiyati.app.ui.screens.admin.AdminMainScreen
import com.yawmiyati.app.ui.screens.auth.ConsentScreen
import com.yawmiyati.app.ui.screens.auth.LoginScreen
import com.yawmiyati.app.ui.screens.auth.RegisterScreen
import com.yawmiyati.app.ui.screens.business.BusinessAssignmentDetailScreen
import com.yawmiyati.app.ui.screens.business.BusinessMainScreen
import com.yawmiyati.app.ui.screens.business.JobApplicationsScreen
import com.yawmiyati.app.ui.screens.business.JobCreateScreen
import com.yawmiyati.app.ui.screens.worker.JobDetailScreen
import com.yawmiyati.app.ui.screens.worker.WorkerAssignmentDetailScreen
import com.yawmiyati.app.ui.screens.worker.WorkerMainScreen

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
                onNavigateToRegister = { navController.navigate(Screen.Consent.route) }
            )
        }

        composable(Screen.Consent.route) {
            ConsentScreen(
                onAccepted = { consentVersion ->
                    navController.navigate("${Screen.Register.route}?cv=$consentVersion")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Screen.Register.route}?cv={cv}",
            arguments = listOf(navArgument("cv") { type = NavType.IntType; defaultValue = 1 })
        ) { backStackEntry ->
            val consentVersion = backStackEntry.arguments?.getInt("cv") ?: 1
            RegisterScreen(
                consentVersion = consentVersion,
                onRegisterSuccess = { role ->
                    val dest = when (role) {
                        "WORKER"   -> Screen.WorkerHome.route
                        "BUSINESS" -> Screen.BusinessHome.route
                        "ADMIN"    -> Screen.AdminVerification.route
                        else       -> Screen.Login.route
                    }
                    navController.navigate(dest) {
                        popUpTo(Screen.Consent.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack(Screen.Login.route, inclusive = false)
                }
            )
        }

        // ── Worker ──────────────────────────────────────────────────────────────

        composable(Screen.WorkerHome.route) {
            WorkerMainScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onManageAssignment = { assignmentId, workerPhone, businessPhone ->
                    navController.navigate(
                        Screen.WorkerAssignmentDetail.createRoute(assignmentId, workerPhone, businessPhone)
                    )
                },
                onNotifications = { navController.navigate(Screen.WorkerNotifications.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.WorkerAssignmentDetail.route,
            arguments = listOf(
                navArgument("assignmentId") { type = NavType.StringType },
                navArgument("workerPhone") { type = NavType.StringType },
                navArgument("businessPhone") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: return@composable
            val workerPhone  = backStackEntry.arguments?.getString("workerPhone") ?: ""
            val businessPhone = backStackEntry.arguments?.getString("businessPhone") ?: ""
            WorkerAssignmentDetailScreen(
                assignmentId  = assignmentId,
                workerPhone   = workerPhone,
                businessPhone = businessPhone,
                onBack        = { navController.popBackStack() }
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

        composable(Screen.WorkerNotifications.route) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        // ── Business ────────────────────────────────────────────────────────────

        composable(Screen.BusinessHome.route) {
            BusinessMainScreen(
                onCreateJob = { navController.navigate(Screen.JobCreate.route) },
                onViewApplications = { jobId ->
                    navController.navigate(Screen.JobApplications.createRoute(jobId))
                },
                onNotifications = { navController.navigate(Screen.WorkerNotifications.route) },
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
                onBack       = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BusinessAssignment.route,
            arguments = listOf(
                navArgument("assignmentId") { type = NavType.StringType },
                navArgument("workerPhone") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: return@composable
            val workerPhone  = backStackEntry.arguments?.getString("workerPhone") ?: ""
            BusinessAssignmentDetailScreen(
                assignmentId = assignmentId,
                workerPhone  = workerPhone,
                onBack       = { navController.popBackStack() }
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
                onManageAssignment = { assignmentId, workerPhone ->
                    navController.navigate(Screen.BusinessAssignment.createRoute(assignmentId, workerPhone))
                }
            )
        }

        // ── Admin ────────────────────────────────────────────────────────────────

        composable(Screen.AdminVerification.route) {
            AdminMainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
