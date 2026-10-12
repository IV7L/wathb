import uuid
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.db import models


class Role(models.TextChoices):
    WORKER = "WORKER", "Worker"
    BUSINESS = "BUSINESS", "Business"
    FUNDER = "FUNDER", "Funder"
    ADMIN = "ADMIN", "Admin"


class UserManager(BaseUserManager):
    def create_user(self, phone, password=None, **extra_fields):
        from accounts.utils import normalize_phone
        if not phone:
            raise ValueError("رقم الهاتف مطلوب.")
        try:
            phone = normalize_phone(phone)
        except ValueError as exc:
            raise ValueError(str(exc)) from exc
        user = self.model(phone=phone, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, phone, password=None, **extra_fields):
        extra_fields.setdefault("is_staff", True)
        extra_fields.setdefault("is_superuser", True)
        extra_fields.setdefault("role", Role.ADMIN)
        extra_fields.setdefault("is_verified", True)
        return self.create_user(phone, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    id = models.UUIDField(primary_key=True, default=uuid.uuid4, editable=False)
    phone = models.CharField(max_length=20, unique=True)
    email = models.EmailField(unique=True, null=True, blank=True)
    role = models.CharField(max_length=20, choices=Role.choices)
    is_verified = models.BooleanField(default=False)
    is_active = models.BooleanField(default=True)
    is_staff = models.BooleanField(default=False)
    date_joined = models.DateTimeField(auto_now_add=True)

    USERNAME_FIELD = "phone"
    REQUIRED_FIELDS = ["role"]

    objects = UserManager()

    class Meta:
        verbose_name = "User"
        verbose_name_plural = "Users"

    def __str__(self):
        return f"{self.phone} ({self.role})"


class VerificationStatus(models.TextChoices):
    PENDING = "PENDING", "Pending"
    APPROVED = "APPROVED", "Approved"
    REJECTED = "REJECTED", "Rejected"
    SUSPENDED = "SUSPENDED", "Suspended"


class WorkerProfile(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="worker_profile")
    full_name = models.CharField(max_length=200)
    location_city = models.CharField(max_length=100, blank=True)
    location_region = models.CharField(max_length=100, blank=True)
    skills = models.TextField(blank=True)
    experience_summary = models.TextField(blank=True)
    national_id_hash = models.CharField(max_length=64, blank=True)
    is_available = models.BooleanField(default=True)

    def __str__(self):
        return f"Worker: {self.full_name}"


class BusinessProfile(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="business_profile")
    legal_name = models.CharField(max_length=300)
    trade_name = models.CharField(max_length=300, blank=True)
    registration_number = models.CharField(max_length=100, blank=True)
    location_address = models.TextField(blank=True)
    location_city = models.CharField(max_length=100, blank=True)
    contact_name = models.CharField(max_length=200)
    contact_phone = models.CharField(max_length=30)
    verification_status = models.CharField(
        max_length=20,
        choices=VerificationStatus.choices,
        default=VerificationStatus.PENDING,
    )
    verified_by = models.ForeignKey(
        User,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="verified_businesses",
    )
    verified_at = models.DateTimeField(null=True, blank=True)
    suspension_reason = models.TextField(blank=True)

    def __str__(self):
        return f"Business: {self.legal_name}"
