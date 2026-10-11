from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers
from accounts.models import User, WorkerProfile, BusinessProfile, Role


class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, validators=[validate_password])
    full_name = serializers.CharField(write_only=True, required=False)
    legal_name = serializers.CharField(write_only=True, required=False)
    contact_name = serializers.CharField(write_only=True, required=False)
    contact_phone = serializers.CharField(write_only=True, required=False)

    class Meta:
        model = User
        fields = ["email", "password", "phone", "role", "full_name", "legal_name", "contact_name", "contact_phone"]

    def validate(self, attrs):
        role = attrs.get("role")
        if role == Role.WORKER and not attrs.get("full_name"):
            raise serializers.ValidationError({"full_name": "Required for workers."})
        if role == Role.WORKER and not attrs.get("phone"):
            raise serializers.ValidationError({"phone": "Required for workers."})
        if role == Role.BUSINESS and not attrs.get("legal_name"):
            raise serializers.ValidationError({"legal_name": "Required for businesses."})
        if role == Role.BUSINESS and not attrs.get("contact_name"):
            raise serializers.ValidationError({"contact_name": "Required for businesses."})
        if role == Role.BUSINESS and not attrs.get("contact_phone"):
            raise serializers.ValidationError({"contact_phone": "Required for businesses."})
        if role not in (Role.WORKER, Role.BUSINESS):
            raise serializers.ValidationError({"role": "Self-registration is only available for workers and businesses."})
        return attrs

    def create(self, validated_data):
        full_name = validated_data.pop("full_name", "")
        legal_name = validated_data.pop("legal_name", "")
        contact_name = validated_data.pop("contact_name", "")
        contact_phone = validated_data.pop("contact_phone", "")
        password = validated_data.pop("password")

        user = User(**validated_data)
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
        fields = ["id", "email", "phone", "role", "is_verified", "date_joined"]
        read_only_fields = ["id", "role", "is_verified", "date_joined"]


class WorkerProfileSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(source="user.email", read_only=True)
    phone = serializers.CharField(source="user.phone", read_only=True)
    is_verified = serializers.BooleanField(source="user.is_verified", read_only=True)

    class Meta:
        model = WorkerProfile
        fields = [
            "id", "email", "phone", "is_verified",
            "full_name", "location_city", "location_region",
            "skills", "experience_summary", "is_available",
        ]
        read_only_fields = ["id", "email", "phone", "is_verified"]


class BusinessProfileSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(source="user.email", read_only=True)
    phone = serializers.CharField(source="user.phone", read_only=True)

    class Meta:
        model = BusinessProfile
        fields = [
            "id", "email", "phone",
            "legal_name", "trade_name", "registration_number",
            "location_address", "location_city",
            "contact_name", "contact_phone",
            "verification_status",
        ]
        read_only_fields = ["id", "email", "phone", "verification_status"]


class AdminBusinessProfileSerializer(serializers.ModelSerializer):
    email = serializers.EmailField(source="user.email", read_only=True)
    user_id = serializers.UUIDField(source="user.id", read_only=True)

    class Meta:
        model = BusinessProfile
        fields = [
            "id", "user_id", "email",
            "legal_name", "trade_name", "registration_number",
            "location_address", "location_city",
            "contact_name", "contact_phone",
            "verification_status", "verified_at", "suspension_reason",
        ]
        read_only_fields = ["id", "user_id", "email"]
