from django.contrib.auth.password_validation import validate_password
from django.utils import timezone
from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from accounts.consent import CONSENT_VERSION
from accounts.models import User, WorkerProfile, BusinessProfile, Role


def _normalize(phone: str) -> str:
    from accounts.utils import normalize_phone
    try:
        return normalize_phone(phone)
    except ValueError as exc:
        raise serializers.ValidationError(str(exc)) from exc


class PhoneTokenObtainPairSerializer(TokenObtainPairSerializer):
    """JWT login serializer that normalizes the phone before authenticate()."""

    def validate(self, attrs):
        raw = attrs.get(self.username_field, "")
        if raw:
            try:
                attrs[self.username_field] = _normalize(raw)
            except serializers.ValidationError:
                pass  # let authenticate() fail naturally → 401
        return super().validate(attrs)


class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, validators=[validate_password])
    consent_version = serializers.IntegerField(write_only=True)
    full_name = serializers.CharField(write_only=True, required=False)
    legal_name = serializers.CharField(write_only=True, required=False)
    contact_name = serializers.CharField(write_only=True, required=False)
    contact_phone = serializers.CharField(write_only=True, required=False)

    class Meta:
        model = User
        fields = [
            "phone", "email", "password", "role", "consent_version",
            "full_name", "legal_name", "contact_name", "contact_phone",
        ]
        extra_kwargs = {"email": {"required": False, "allow_null": True}}

    def validate_phone(self, value):
        normalized = _normalize(value)
        if User.objects.filter(phone=normalized).exists():
            raise serializers.ValidationError("رقم الهاتف هذا مسجّل مسبقاً.")
        return normalized

    def validate_email(self, value):
        if value == "":
            return None
        return value

    def validate_consent_version(self, value):
        if value != CONSENT_VERSION:
            raise serializers.ValidationError(
                f"يجب الموافقة على الشروط الحالية (الإصدار {CONSENT_VERSION})."
            )
        return value

    def validate(self, attrs):
        role = attrs.get("role")
        if role == Role.WORKER and not attrs.get("full_name"):
            raise serializers.ValidationError({"full_name": "Required for workers."})
        if role == Role.BUSINESS and not attrs.get("legal_name"):
            raise serializers.ValidationError({"legal_name": "Required for businesses."})
        if role == Role.BUSINESS and not attrs.get("contact_name"):
            raise serializers.ValidationError({"contact_name": "Required for businesses."})
        if role == Role.BUSINESS and not attrs.get("contact_phone"):
            raise serializers.ValidationError({"contact_phone": "Required for businesses."})
        if role not in (Role.WORKER, Role.BUSINESS):
            raise serializers.ValidationError(
                {"role": "Self-registration is only available for workers and businesses."}
            )
        return attrs

    def create(self, validated_data):
        full_name = validated_data.pop("full_name", "")
        legal_name = validated_data.pop("legal_name", "")
        contact_name = validated_data.pop("contact_name", "")
        contact_phone = validated_data.pop("contact_phone", "")
        password = validated_data.pop("password")
        email = validated_data.pop("email", None) or None
        validated_data.pop("consent_version", None)  # already validated; stored explicitly below

        user = User(**validated_data)
        user.email = email
        user.consent_version = CONSENT_VERSION
        user.accepted_at = timezone.now()
        user.set_password(password)
        user.save()

        if user.role == Role.WORKER:
            WorkerProfile.objects.create(user=user, full_name=full_name)
        elif user.role == Role.BUSINESS:
            BusinessProfile.objects.create(
                user=user,
                legal_name=legal_name,
                contact_name=contact_name,
                contact_phone=contact_phone,
            )
        return user


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["id", "phone", "email", "role", "is_verified", "date_joined",
                  "consent_version", "accepted_at"]
        read_only_fields = ["id", "role", "is_verified", "date_joined",
                            "consent_version", "accepted_at"]


class WorkerProfileSerializer(serializers.ModelSerializer):
    phone = serializers.CharField(source="user.phone", read_only=True)
    email = serializers.EmailField(source="user.email", read_only=True, allow_null=True, default=None)
    is_verified = serializers.BooleanField(source="user.is_verified", read_only=True)

    class Meta:
        model = WorkerProfile
        fields = [
            "id", "phone", "email", "is_verified",
            "full_name", "location_city", "location_region",
            "skills", "experience_summary", "is_available",
        ]
        read_only_fields = ["id", "phone", "email", "is_verified"]


class AdminWorkerProfileSerializer(serializers.ModelSerializer):
    phone = serializers.CharField(source="user.phone", read_only=True)
    email = serializers.EmailField(source="user.email", read_only=True, allow_null=True, default=None)
    user_id = serializers.UUIDField(source="user.id", read_only=True)
    is_verified = serializers.BooleanField(source="user.is_verified", read_only=True)

    class Meta:
        model = WorkerProfile
        fields = [
            "id", "user_id", "phone", "email", "is_verified",
            "full_name", "location_city", "location_region",
            "skills", "experience_summary", "is_available",
        ]
        read_only_fields = fields


class BusinessProfileSerializer(serializers.ModelSerializer):
    phone = serializers.CharField(source="user.phone", read_only=True)
    email = serializers.EmailField(source="user.email", read_only=True, allow_null=True, default=None)

    class Meta:
        model = BusinessProfile
        fields = [
            "id", "phone", "email",
            "legal_name", "trade_name", "registration_number",
            "location_address", "location_city",
            "contact_name", "contact_phone",
            "verification_status",
        ]
        read_only_fields = ["id", "phone", "email", "verification_status"]


class AdminBusinessProfileSerializer(serializers.ModelSerializer):
    phone = serializers.CharField(source="user.phone", read_only=True)
    email = serializers.EmailField(source="user.email", read_only=True, allow_null=True, default=None)
    user_id = serializers.UUIDField(source="user.id", read_only=True)

    class Meta:
        model = BusinessProfile
        fields = [
            "id", "user_id", "phone", "email",
            "legal_name", "trade_name", "registration_number",
            "location_address", "location_city",
            "contact_name", "contact_phone",
            "verification_status", "verified_at", "suspension_reason",
        ]
        read_only_fields = ["id", "user_id", "phone", "email"]
