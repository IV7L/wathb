import uuid
from django.db import models
from django.conf import settings


class NotificationType(models.TextChoices):
    APPLICATION_ACCEPTED = "APPLICATION_ACCEPTED", "Application Accepted"
    APPLICATION_REJECTED = "APPLICATION_REJECTED", "Application Rejected"
    JOB_OFFER = "JOB_OFFER", "Job Offer"
    ATTENDANCE_DISPUTED = "ATTENDANCE_DISPUTED", "Attendance Disputed"
    COMPLETION_CONFIRMED = "COMPLETION_CONFIRMED", "Work Completion Confirmed"
    COMPLETION_DISPUTED = "COMPLETION_DISPUTED", "Work Completion Disputed"
    WAGE_DUE = "WAGE_DUE", "Wage Due"
    PAYMENT_DECLARED = "PAYMENT_DECLARED", "Payment Declared"
    PAYMENT_VERIFIED = "PAYMENT_VERIFIED", "Payment Verified"
    PAYMENT_REJECTED = "PAYMENT_REJECTED", "Payment Rejected"
    DISPUTE_STATUS_CHANGED = "DISPUTE_STATUS_CHANGED", "Dispute Status Changed"
    FUNDING_APPROVED = "FUNDING_APPROVED", "Funding Approved"
    FUNDING_REJECTED = "FUNDING_REJECTED", "Funding Rejected"


class Notification(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    recipient = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name="notifications",
    )
    notification_type = models.CharField(max_length=50, choices=NotificationType.choices)
    title = models.CharField(max_length=200)
    body = models.TextField()
    related_model = models.CharField(max_length=100, blank=True)
    related_id = models.CharField(max_length=100, blank=True)
    is_read = models.BooleanField(default=False)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"{self.notification_type} → {self.recipient.email}"
