package com.yawmiyati.app.data.api

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val phone: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class RefreshRequest(val refresh: String)
data class RefreshResponse(val access: String, val refresh: String? = null)

data class RegisterWorkerRequest(
    val phone: String,
    val password: String,
    val role: String = "WORKER",
    @SerializedName("full_name") val fullName: String,
    @SerializedName("consent_version") val consentVersion: Int,
)

data class RegisterBusinessRequest(
    val phone: String,
    val password: String,
    val role: String = "BUSINESS",
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("contact_phone") val contactPhone: String,
    val email: String? = null,
    @SerializedName("consent_version") val consentVersion: Int,
)

// RegisterView returns {user, access, refresh} on 201
data class RegisterResponse(
    val user: UserResponse,
    val access: String,
    val refresh: String
)

data class UserResponse(
    val id: String,
    val email: String? = null,
    val phone: String,
    val role: String,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("date_joined") val dateJoined: String
)

// ── Jobs ──────────────────────────────────────────────────────────────────────

data class BusinessTrackRecord(
    val label: String,
    @SerializedName("total_paid_days") val totalPaidDays: Int,
    @SerializedName("on_time_days") val onTimeDays: Int,
)

// Matches JobListingSerializer fields exactly
data class JobListingResponse(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("business_name") val businessName: String,
    @SerializedName("business_verification_status") val businessVerificationStatus: String,
    @SerializedName("business_track_record") val businessTrackRecord: BusinessTrackRecord? = null,
    @SerializedName("location_city") val locationCity: String,
    @SerializedName("location_address") val locationAddress: String,
    @SerializedName("work_date") val workDate: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("num_workers_needed") val numWorkersNeeded: Int,
    @SerializedName("required_skills") val requiredSkills: String,
    @SerializedName("wage_amount") val wageAmount: Long,
    @SerializedName("wage_currency") val wageCurrency: String,
    @SerializedName("wage_basis") val wageBasis: String,
    @SerializedName("wage_display") val wageDisplay: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_deadline_days") val paymentDeadlineDays: Int,
    @SerializedName("attendance_requirements") val attendanceRequirements: String,
    @SerializedName("cancellation_policy") val cancellationPolicy: String,
    @SerializedName("additional_conditions") val additionalConditions: String,
    val category: String? = null,
    @SerializedName("is_new") val isNew: Boolean = false,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    val status: String,
    @SerializedName("is_funded") val isFunded: Boolean,
    @SerializedName("published_at") val publishedAt: String?,
    val version: Int,
    @SerializedName("created_at") val createdAt: String
)

// ── Worker profile ────────────────────────────────────────────────────────────

data class WorkerProfileResponse(
    val id: Int,
    val phone: String,
    val email: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("location_city") val locationCity: String,
    @SerializedName("location_region") val locationRegion: String,
    val skills: String,
    @SerializedName("experience_summary") val experienceSummary: String,
    @SerializedName("is_available") val isAvailable: Boolean,
)

data class WorkerProfileUpdateRequest(
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("location_city") val locationCity: String? = null,
    @SerializedName("location_region") val locationRegion: String? = null,
    @SerializedName("is_available") val isAvailable: Boolean? = null,
)

data class BusinessProfileResponse(
    val id: String,
    val phone: String,
    val email: String? = null,
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("trade_name") val tradeName: String,
    @SerializedName("registration_number") val registrationNumber: String,
    @SerializedName("location_address") val locationAddress: String,
    @SerializedName("location_city") val locationCity: String,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("contact_phone") val contactPhone: String,
    @SerializedName("verification_status") val verificationStatus: String,
)

data class BusinessProfileUpdateRequest(
    @SerializedName("legal_name") val legalName: String? = null,
    @SerializedName("trade_name") val tradeName: String? = null,
    @SerializedName("location_address") val locationAddress: String? = null,
    @SerializedName("location_city") val locationCity: String? = null,
    @SerializedName("contact_name") val contactName: String? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null,
)

// Matches JobListingCreateSerializer — wage_amount in minor units (agorot/cents)
data class JobCreateRequest(
    val title: String,
    val description: String,
    @SerializedName("location_address") val locationAddress: String,
    @SerializedName("location_city") val locationCity: String,
    @SerializedName("work_date") val workDate: String,          // YYYY-MM-DD
    @SerializedName("start_time") val startTime: String,        // HH:MM
    @SerializedName("end_time") val endTime: String,            // HH:MM
    @SerializedName("num_workers_needed") val numWorkersNeeded: Int,
    @SerializedName("wage_amount") val wageAmount: Long,        // minor units
    @SerializedName("wage_currency") val wageCurrency: String = "ILS",
    @SerializedName("wage_basis") val wageBasis: String = "PER_DAY",
    @SerializedName("payment_method") val paymentMethod: String = "CASH",
    @SerializedName("payment_deadline_days") val paymentDeadlineDays: Int = 3,
    @SerializedName("required_skills") val requiredSkills: String = "",
    @SerializedName("attendance_requirements") val attendanceRequirements: String = "",
    @SerializedName("cancellation_policy") val cancellationPolicy: String = "",
    @SerializedName("additional_conditions") val additionalConditions: String = "",
    @SerializedName("is_funded") val isFunded: Boolean = false
)

// Sent when a worker applies to a job; cover_note is optional
data class ApplyRequest(
    @SerializedName("cover_note") val coverNote: String = ""
)

// Matches JobApplicationSerializer (assignment_id is null until job is offered)
data class JobApplicationResponse(
    val id: String,
    val job: String,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("worker_name") val workerName: String? = null,
    @SerializedName("worker_phone") val workerPhone: String? = null,
    @SerializedName("worker_is_verified") val workerIsVerified: Boolean = false,
    val status: String,
    @SerializedName("cover_note") val coverNote: String,
    @SerializedName("applied_at") val appliedAt: String,
    @SerializedName("reviewed_at") val reviewedAt: String?,
    @SerializedName("assignment_id") val assignmentId: String? = null,
    @SerializedName("assignment_status") val assignmentStatus: String? = null,
)

// ── Generic ──────────────────────────────────────────────────────────────────

data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

data class ApiError(
    val detail: String?,
    val error: String?,
    val fields: Map<String, List<String>>?,
    val nonFieldErrors: List<String>? = null
)

// ── Admin ─────────────────────────────────────────────────────────────────────

// Matches AdminBusinessProfileSerializer
data class AdminBusinessProfile(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    val email: String? = null,
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("trade_name") val tradeName: String? = null,
    @SerializedName("registration_number") val registrationNumber: String? = null,
    @SerializedName("location_address") val locationAddress: String? = null,
    @SerializedName("location_city") val locationCity: String? = null,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("contact_phone") val contactPhone: String,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("verified_at") val verifiedAt: String? = null,
    @SerializedName("suspension_reason") val suspensionReason: String? = null
)

data class AdminVerifyRequest(
    val action: String,   // "approve", "reject", or "suspend"
    val reason: String = ""
)

// Matches AdminWorkerProfileSerializer
data class AdminWorkerProfile(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    val phone: String,
    val email: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("location_city") val locationCity: String,
    @SerializedName("location_region") val locationRegion: String,
)

data class AdminWorkerActionRequest(
    val action: String,   // "verify" or "suspend"
)

// Matches _compute_pilot_summary() in reporting/views.py
data class PilotSummaryResponse(
    @SerializedName("verified_businesses")         val verifiedBusinesses: Int,
    @SerializedName("verified_workers")            val verifiedWorkers: Int,
    @SerializedName("published_jobs")              val publishedJobs: Int,
    @SerializedName("applications")                val applications: Int,
    @SerializedName("accepted_assignments")        val acceptedAssignments: Int,
    @SerializedName("completed_worker_days")       val completedWorkerDays: Int,
    @SerializedName("wage_obligations_created")    val wageObligationsCreated: Int,
    @SerializedName("payments_declared")           val paymentsDeclared: Int,
    @SerializedName("payments_verified_paid")      val paymentsVerifiedPaid: Int,
    @SerializedName("total_verified_paid_minor_units") val totalVerifiedPaidMinor: Long,
    @SerializedName("disputes_opened")             val disputesOpened: Int,
    @SerializedName("disputes_resolved")           val disputesResolved: Int,
)

// ── Assignments ───────────────────────────────────────────────────────────────

// Matches AssignmentSerializer
data class AssignmentResponse(
    val id: String,
    val job: String,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("worker_phone") val workerPhone: String,
    @SerializedName("business_name") val businessName: String,
    // Normalized E.164 phone of the business user, ready for tel: and wa.me links.
    @SerializedName("business_phone") val businessPhone: String? = null,
    // Live job terms — populated on every response for pre-acceptance review
    @SerializedName("job_work_date") val jobWorkDate: String? = null,
    @SerializedName("job_start_time") val jobStartTime: String? = null,
    @SerializedName("job_end_time") val jobEndTime: String? = null,
    @SerializedName("job_location_city") val jobLocationCity: String? = null,
    @SerializedName("job_location_address") val jobLocationAddress: String? = null,
    @SerializedName("job_wage_amount") val jobWageAmount: Long? = null,
    @SerializedName("job_wage_currency") val jobWageCurrency: String? = null,
    @SerializedName("job_wage_basis") val jobWageBasis: String? = null,
    @SerializedName("job_payment_method") val jobPaymentMethod: String? = null,
    @SerializedName("job_payment_deadline_days") val jobPaymentDeadlineDays: Int? = null,
    val status: String,
    // Populated by backend on accept; null on OFFERED/DECLINED
    @SerializedName("terms_snapshot") val termsSnapshot: Map<String, Any>? = null,
    @SerializedName("offered_at") val offeredAt: String?,
    @SerializedName("accepted_at") val acceptedAt: String?,
    @SerializedName("cancelled_at") val cancelledAt: String?,
    @SerializedName("cancellation_reason") val cancellationReason: String
)

// Sent by business when acting on a job application
data class ApplicationActionRequest(
    val action: String   // "offer" or "reject"
)

// ── Attendance ────────────────────────────────────────────────────────────────

// Matches AttendanceRecordSerializer
data class AttendanceResponse(
    val id: String,
    val assignment: String,
    @SerializedName("submitted_by_phone") val submittedByPhone: String,
    @SerializedName("submitted_at") val submittedAt: String,
    @SerializedName("work_date") val workDate: String,
    @SerializedName("check_in_time") val checkInTime: String?,
    @SerializedName("check_out_time") val checkOutTime: String?,
    val notes: String,
    val status: String,
    @SerializedName("confirmed_at") val confirmedAt: String?
)

data class AttendanceSubmitRequest(
    @SerializedName("work_date") val workDate: String,
    @SerializedName("check_in_time") val checkInTime: String? = null,
    @SerializedName("check_out_time") val checkOutTime: String? = null,
    val notes: String = ""
)

// ── Completion ────────────────────────────────────────────────────────────────

// Matches CompletionRecordSerializer
data class CompletionResponse(
    val id: String,
    val assignment: String,
    @SerializedName("submitted_by_phone") val submittedByPhone: String,
    @SerializedName("submitted_at") val submittedAt: String,
    @SerializedName("hours_worked") val hoursWorked: String?,
    val notes: String,
    val status: String,
    @SerializedName("confirmed_at") val confirmedAt: String?
)

data class CompletionSubmitRequest(
    @SerializedName("hours_worked") val hoursWorked: String? = null,
    val notes: String = ""
)

// ── Wages ─────────────────────────────────────────────────────────────────────

// Matches WageObligationSerializer (includes nested declarations)
data class WageObligationResponse(
    val id: String,
    val assignment: String,
    @SerializedName("worker_phone") val workerPhone: String,
    @SerializedName("worker_name") val workerName: String? = null,
    @SerializedName("job_title") val jobTitle: String,
    val amount: Long,
    val currency: String,
    @SerializedName("amount_display") val amountDisplay: String,
    @SerializedName("due_date") val dueDate: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    val declarations: List<PaymentDeclarationResponse> = emptyList()
)

// Matches PaymentDeclarationSerializer
data class PaymentDeclarationResponse(
    val id: String,
    @SerializedName("wage_obligation") val wageObligation: String,
    @SerializedName("declared_amount") val declaredAmount: Long,
    val currency: String,
    @SerializedName("payment_date") val paymentDate: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("transaction_reference") val transactionReference: String,
    @SerializedName("declared_by_phone") val declaredByPhone: String,
    @SerializedName("declared_at") val declaredAt: String,
    val status: String,
    @SerializedName("review_notes") val reviewNotes: String
)

// Matches PaymentDeclarationCreateSerializer
data class DeclarePaymentRequest(
    @SerializedName("declared_amount") val declaredAmount: Long,
    val currency: String = "ILS",
    @SerializedName("payment_date") val paymentDate: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("transaction_reference") val transactionReference: String = ""
)

data class DisputePaymentRequest(
    val category: String = "PAYMENT_NOT_RECEIVED",
    val description: String
)

// ── Disputes ──────────────────────────────────────────────────────────────────

data class DisputeResponse(
    val id: String,
    val category: String,
    val assignment: String,
    @SerializedName("wage_obligation") val wageObligation: String? = null,
    @SerializedName("raised_by_phone") val raisedByPhone: String,
    val description: String,
    val status: String,
    @SerializedName("assigned_to_phone") val assignedToPhone: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("resolved_at") val resolvedAt: String? = null,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null,
)

data class DisputeResolveRequest(
    val action: String,
    val notes: String,
)

// ── Notifications ─────────────────────────────────────────────────────────────

data class NotificationResponse(
    val id: String,
    @SerializedName("notification_type") val notificationType: String,
    val title: String,
    val body: String,
    @SerializedName("related_model") val relatedModel: String? = null,
    @SerializedName("related_id") val relatedId: String? = null,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String,
)

// ── Admin verify declaration ───────────────────────────────────────────────────

// Sent to admin verify endpoint
data class AdminVerifyDeclarationRequest(
    val action: String,  // "verify" or "reject"
    @SerializedName("review_notes") val reviewNotes: String = ""
)

data class CancelRequest(val reason: String)
