package com.yawmiyati.app.navigation

sealed class Screen(val route: String) {
    object Login              : Screen("login")
    object Register           : Screen("register")
    object WorkerHome         : Screen("worker_home")
    object WorkerAssignments  : Screen("worker_assignments")
    object WorkerWages        : Screen("worker_wages")
    object BusinessHome       : Screen("business_home")
    object JobCreate          : Screen("job_create")
    object BusinessWages      : Screen("business_wages")
    object AdminVerification  : Screen("admin_verification")
    object AdminWages         : Screen("admin_wages")
    object JobDetail          : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object JobApplications    : Screen("job_applications/{jobId}") {
        fun createRoute(jobId: String) = "job_applications/$jobId"
    }
    object BusinessAssignment : Screen("business_assignment/{assignmentId}/{workerEmail}") {
        fun createRoute(assignmentId: String, workerEmail: String) =
            "business_assignment/$assignmentId/${workerEmail.replace("/", "%2F")}"
    }
    object WorkerAssignmentDetail : Screen("worker_assignment/{assignmentId}/{workerEmail}") {
        fun createRoute(assignmentId: String, workerEmail: String) =
            "worker_assignment/$assignmentId/${workerEmail.replace("/", "%2F")}"
    }
}
