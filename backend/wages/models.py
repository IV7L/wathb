import uuid
import os
from django.db import models
from django.conf import settings
from jobs.models import PaymentMethod  # noqa: F401 — re-exported for wages domain


class WageObligationStatus(models.TextChoices):
    PENDING = "PENDING", "Pending"
    DUE = "DUE", "Due"
    DECLARED = "DECLARED", "Payment Declared"
    AWAITING_ACK = "AWAITING_ACK", "Awaiting Worker Acknowledgement"
    UNDER_REVIEW = "UNDER_REVIEW", "Under Review"
    DISPUTED = "DISPUTED", "Disputed"
    VERIFIED_PAID = "VERIFIED_PAID", "Verified Paid"
    REJECTED = "REJECTED", "Declaration Rejected"
    CANCELLED = "CANCELLED", "Cancelled"


class PaymentDeclarationStatus(models.TextChoices):
    SUBMITTED = "SUBMITTED", "Submitted"
    ACKNOWLEDGED = "ACKNOWLEDGED", "Acknowledged by Worker"
    DISPUTED = "DISPUTED", "Disputed by Worker"
    VERIFIED = "VERIFIED", "Verified by Staff"
    REJECTED = "REJECTED", "Rejected by Staff"


class WageObligation(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    assignment = models.OneToOneField(
        "assignments.Assignment",
        on_delete=models.PROTECT,
        related_name="wage_obligation",
    )
    amount = models.PositiveBigIntegerField(help_text="Amount in minor currency units")
    currency = models.CharField(max_length=3)
    due_date = models.DateField()
    status = models.CharField(
        max_length=20,
        choices=WageObligationStatus.choices,
        default=WageObligationStatus.PENDING,
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-created_at"]

    def __str__(self):
        return f"Wage {self.amount} {self.currency} — {self.assignment} ({self.status})"


def _evidence_upload_path(instance, filename):
    ext = os.path.splitext(filename)[1].lower()
    return f"evidence/{instance.declaration.id}/{uuid.uuid4()}{ext}"


class PaymentDeclaration(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    wage_obligation = models.ForeignKey(
        WageObligation,
        on_delete=models.PROTECT,
        related_name="declarations",
    )
    declared_amount = models.PositiveBigIntegerField(help_text="Amount in minor currency units")
    currency = models.CharField(max_length=3)
    payment_date = models.DateField()
    payment_method = models.CharField(max_length=30, choices=PaymentMethod.choices)
    transaction_reference = models.CharField(max_length=200, blank=True)
    declared_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="payment_declarations",
    )
    declared_at = models.DateTimeField(auto_now_add=True)
    status = models.CharField(
        max_length=20,
        choices=PaymentDeclarationStatus.choices,
        default=PaymentDeclarationStatus.SUBMITTED,
    )
    reviewed_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        null=True,
        blank=True,
        related_name="reviewed_declarations",
    )
    reviewed_at = models.DateTimeField(null=True, blank=True)
    review_notes = models.TextField(blank=True)

    class Meta:
        ordering = ["-declared_at"]

    def __str__(self):
        return f"Declaration {self.declared_amount} {self.currency} by {self.declared_by.email} ({self.status})"


class PaymentEvidence(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    declaration = models.ForeignKey(PaymentDeclaration, on_delete=models.PROTECT, related_name="evidence_files")
    file = models.FileField(upload_to=_evidence_upload_path)
    original_filename = models.CharField(max_length=255)
    content_type = models.CharField(max_length=100)
    file_size = models.PositiveBigIntegerField()
    uploaded_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.PROTECT,
        related_name="uploaded_evidence",
    )
    uploaded_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Evidence: {self.original_filename} for {self.declaration}"
