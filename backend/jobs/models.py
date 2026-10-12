import uuid
from django.db import models
from django.conf import settings


class JobCategory(models.TextChoices):
    LOADING      = "LOADING",      "تحميل وتنزيل"
    CLEANING     = "CLEANING",     "تنظيف"
    CONSTRUCTION = "CONSTRUCTION", "بناء وترميم"
    AGRICULTURE  = "AGRICULTURE",  "زراعة"
    DELIVERY     = "DELIVERY",     "نقل وتوصيل"
    FOOD         = "FOOD",         "مطاعم ومطابخ"
    RETAIL       = "RETAIL",       "محلات وبيع"
    OTHER        = "OTHER",        "أخرى"


class WageBasis(models.TextChoices):
    PER_DAY = "PER_DAY", "Per Day"
    PER_TASK = "PER_TASK", "Per Task"
    PER_HOUR = "PER_HOUR", "Per Hour"


class PaymentMethod(models.TextChoices):
    CASH = "CASH", "Cash"
    BANK_TRANSFER = "BANK_TRANSFER", "Bank Transfer"
    OTHER = "OTHER", "Other"


class Currency(models.TextChoices):
    ILS = "ILS", "Israeli New Shekel"
    USD = "USD", "US Dollar"


class JobListingStatus(models.TextChoices):
    DRAFT = "DRAFT", "Draft"
    PENDING_REVIEW = "PENDING_REVIEW", "Pending Review"
    PUBLISHED = "PUBLISHED", "Published"
    FILLED = "FILLED", "Filled"
    IN_PROGRESS = "IN_PROGRESS", "In Progress"
    COMPLETED = "COMPLETED", "Completed"
    CANCELLED = "CANCELLED", "Cancelled"


VALID_JOB_TRANSITIONS = {
    JobListingStatus.DRAFT: [JobListingStatus.PENDING_REVIEW, JobListingStatus.CANCELLED],
    JobListingStatus.PENDING_REVIEW: [JobListingStatus.PUBLISHED, JobListingStatus.DRAFT, JobListingStatus.CANCELLED],
    JobListingStatus.PUBLISHED: [JobListingStatus.FILLED, JobListingStatus.CANCELLED],
    JobListingStatus.FILLED: [JobListingStatus.IN_PROGRESS, JobListingStatus.CANCELLED],
    JobListingStatus.IN_PROGRESS: [JobListingStatus.COMPLETED, JobListingStatus.CANCELLED],
    JobListingStatus.COMPLETED: [],
    JobListingStatus.CANCELLED: [],
}


class ApplicationStatus(models.TextChoices):
    PENDING = "PENDING", "Pending"
    OFFERED = "OFFERED", "Offered"
    ACCEPTED = "ACCEPTED", "Accepted"
    REJECTED = "REJECTED", "Rejected"
    WITHDRAWN = "WITHDRAWN", "Withdrawn"
    EXPIRED = "EXPIRED", "Expired"


class JobListing(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    business = models.ForeignKey(
        "accounts.BusinessProfile",
        on_delete=models.PROTECT,
        related_name="job_listings",
    )
    category = models.CharField(
        max_length=20, choices=JobCategory.choices, default=JobCategory.OTHER
    )
    title = models.CharField(max_length=300)
    description = models.TextField()
    location_address = models.CharField(max_length=500)
    location_city = models.CharField(max_length=100)
    work_date = models.DateField()
    start_time = models.TimeField()
    end_time = models.TimeField()
    num_workers_needed = models.PositiveIntegerField(default=1)
    required_skills = models.TextField(blank=True)
    wage_amount = models.PositiveBigIntegerField(help_text="Amount in minor currency units (e.g. agorot for ILS)")
    wage_currency = models.CharField(max_length=3, choices=Currency.choices, default=Currency.ILS)
    wage_basis = models.CharField(max_length=20, choices=WageBasis.choices, default=WageBasis.PER_DAY)
    payment_method = models.CharField(max_length=30, choices=PaymentMethod.choices, default=PaymentMethod.CASH)
    payment_deadline_days = models.PositiveIntegerField(default=3, help_text="Days after completion")
    attendance_requirements = models.TextField(blank=True)
    cancellation_policy = models.TextField(blank=True)
    additional_conditions = models.TextField(blank=True)
    status = models.CharField(
        max_length=20,
        choices=JobListingStatus.choices,
        default=JobListingStatus.DRAFT,
    )
    is_funded = models.BooleanField(default=False)
    published_at = models.DateTimeField(null=True, blank=True)
    version = models.PositiveIntegerField(default=1)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.title} — {self.business.legal_name} ({self.work_date})"

    def can_transition_to(self, new_status):
        return new_status in VALID_JOB_TRANSITIONS.get(self.status, [])

    def material_terms_snapshot(self):
        return {
            "job_id": str(self.id),
            "title": self.title,
            "description": self.description,
            "business_id": str(self.business.id),
            "business_name": self.business.legal_name,
            "business_verification_status": self.business.verification_status,
            "location_address": self.location_address,
            "location_city": self.location_city,
            "work_date": str(self.work_date),
            "start_time": str(self.start_time),
            "end_time": str(self.end_time),
            "num_workers_needed": self.num_workers_needed,
            "required_skills": self.required_skills,
            "wage_amount": self.wage_amount,
            "wage_currency": self.wage_currency,
            "wage_basis": self.wage_basis,
            "payment_method": self.payment_method,
            "payment_deadline_days": self.payment_deadline_days,
            "attendance_requirements": self.attendance_requirements,
            "cancellation_policy": self.cancellation_policy,
            "additional_conditions": self.additional_conditions,
            "listing_version": self.version,
        }


class JobApplication(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    job = models.ForeignKey(JobListing, on_delete=models.PROTECT, related_name="applications")
    worker = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="job_applications",
    )
    status = models.CharField(
        max_length=20,
        choices=ApplicationStatus.choices,
        default=ApplicationStatus.PENDING,
    )
    cover_note = models.TextField(blank=True)
    applied_at = models.DateTimeField(auto_now_add=True)
    reviewed_at = models.DateTimeField(null=True, blank=True)

    class Meta:
        unique_together = [("job", "worker")]
        ordering = ["-applied_at"]

    def __str__(self):
        return f"{self.worker.email} → {self.job.title} ({self.status})"
