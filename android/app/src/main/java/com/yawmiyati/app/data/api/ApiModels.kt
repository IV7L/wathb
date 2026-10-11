package com.yawmiyati.app.data.api

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class RefreshRequest(val refresh: String)
data class RefreshResponse(val access: String, val refresh: String? = null)

data class RegisterWorkerRequest(
    val email: String,
    val password: String,
    val role: String = "WORKER",
    @SerializedName("full_name") val fullName: String,
    val phone: String? = null
)

data class RegisterBusinessRequest(
    val email: String,
    val password: String,
    val role: String = "BUSINESS",
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("contact_phone") val contactPhone: String,
    val phone: String? = null
)

// RegisterView returns {user, access, refresh} on 201
data class RegisterResponse(
    val user: UserResponse,
    val access: String,
    val refresh: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val phone: String?,
    val role: String,
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("date_joined") val dateJoined: String
)

// ── Jobs ──────────────────────────────────────────────────────────────────────

// Matches JobListingSerializer fields exactly
data class JobListingResponse(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("business_name") val businessName: String,
    @SerializedName("business_verification_status") val businessVerificationStatus: String,
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
    val status: String,
    @SerializedName("is_funded") val isFunded: Boolean,
    @SerializedName("published_at") val publishedAt: String?,
    val version: Int,
    @SerializedName("created_at") val createdAt: String
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
    @SerializedName("worker_email") val workerEmail: String,
    @SerializedName("worker_name") val workerName: String? = null,
    @SerializedName("worker_phone") val workerPhone: String? = null,
    val status: String,
    @SerializedName("cover_note") val coverNote: String,
    @SerializedName("applied_at") val appliedAt: String,
    @SerializedName("reviewed_at") val reviewedAt: String?,
    @SerializedName("assignment_id") val assignmentId: String? = null
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
    val email: String,
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("trade_name") val tradeName: String?,
    @SerializedName("registration_number") val registrationNumber: String?,
    @SerializedName("location_address") val locationAddress: String?,
    @SerializedName("location_city") val locationCity: String?,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("contact_phone") val contactPhone: String,
    @SerializedName("verification_status") val verificationStatus: String,
    @SerializedName("verified_at") val verifiedAt: String?,
    @SerializedName("suspension_reason") val suspensionReason: String
)

data class AdminVerifyRequest(
    val action: String,   // "approve", "reject", or "suspend"
    val reason: String = ""
)

// ── Assignments ───────────────────────────────────────────────────────────────

// Matches AssignmentSerializer
data class AssignmentResponse(
    val id: String,
    val job: String,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("worker_email") val workerEmail: String,
    @SerializedName("business_name") val businessName: String,
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
    @SerializedName("submitted_by_email") val submittedByEmail: String,
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
    @SerializedName("submitted_by_email") val submittedByEmail: String,
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
    @SerializedName("worker_email") val workerEmail: String,
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
    @SerializedName("declared_by_email") val declaredByEmail: String,
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

// Sent to admin verify endpoint
data class AdminVerifyDeclarationRequest(
    val action: String,  // "verify" or "reject"
    @SerializedName("review_notes") val reviewNotes: String = ""
)
