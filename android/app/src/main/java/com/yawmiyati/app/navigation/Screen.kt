package com.yawmiyati.app.navigation

sealed class Screen(val route: String) {
    object Login              : Screen("login")
    object Consent            : Screen("consent")
    object Register           : Screen("register")
    object WorkerHome         : Screen("worker_home")
    object WorkerProfile      : Screen("worker_profile")
    object WorkerApplications : Screen("worker_applications")
    object WorkerAssignments  : Screen("worker_assignments")
    object WorkerWages        : Screen("worker_wages")
    object BusinessHome       : Screen("business_home")
    object JobCreate          : Screen("job_create")
    object BusinessWages      : Screen("business_wages")
    object AdminVerification  : Screen("admin_verification")
    object AdminWages         : Screen("admin_wages")
    object AdminDisputes      : Screen("admin_disputes")
    object WorkerNotifications : Screen("worker_notifications")
    object JobDetail          : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object JobApplications    : Screen("job_applications/{jobId}") {
        fun createRoute(jobId: String) = "job_applications/$jobId"
    }
    object BusinessAssignment : Screen("business_assignment/{assignmentId}/{workerPhone}") {
        fun createRoute(assignmentId: String, workerPhone: String) =
            "business_assignment/$assignmentId/${workerPhone.replace("/", "%2F")}"
    }
    object WorkerAssignmentDetail : Screen("worker_assignment/{assignmentId}/{workerPhone}/{businessPhone}") {
        fun createRoute(assignmentId: String, workerPhone: String, businessPhone: String) =
            "worker_assignment/$assignmentId/${workerPhone.replace("/", "%2F")}/${businessPhone.replace("/", "%2F").replace("+", "%2B")}"
    }
}
