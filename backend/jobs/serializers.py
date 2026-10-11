from rest_framework import serializers
from jobs.models import JobListing, JobApplication


class JobListingSerializer(serializers.ModelSerializer):
    business_name = serializers.CharField(source="business.legal_name", read_only=True)
    business_verification_status = serializers.CharField(source="business.verification_status", read_only=True)
    wage_display = serializers.SerializerMethodField()

    class Meta:
        model = JobListing
        fields = [
            "id", "business_name", "business_verification_status",
            "title", "description", "location_address", "location_city",
            "work_date", "start_time", "end_time", "num_workers_needed",
            "required_skills", "wage_amount", "wage_currency", "wage_basis",
            "wage_display", "payment_method", "payment_deadline_days",
            "attendance_requirements", "cancellation_policy", "additional_conditions",
            "status", "is_funded", "published_at", "version", "created_at",
        ]
        read_only_fields = ["id", "business_name", "business_verification_status", "status", "published_at", "version", "created_at"]

    def get_wage_display(self, obj):
        major = obj.wage_amount / 100
        return f"{major:.0f} {obj.wage_currency} / {obj.get_wage_basis_display()}"


class JobListingCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = JobListing
        fields = [
            "title", "description", "location_address", "location_city",
            "work_date", "start_time", "end_time", "num_workers_needed",
            "required_skills", "wage_amount", "wage_currency", "wage_basis",
            "payment_method", "payment_deadline_days",
            "attendance_requirements", "cancellation_policy", "additional_conditions",
            "is_funded",
        ]

    def validate_wage_amount(self, value):
        if value <= 0:
            raise serializers.ValidationError("Wage amount must be positive.")
        return value

    def validate(self, attrs):
        start = attrs.get("start_time")
        end = attrs.get("end_time")
        if start and end and end <= start:
            raise serializers.ValidationError({"end_time": "End time must be after start time."})
        return attrs


class JobApplicationSerializer(serializers.ModelSerializer):
    worker_email = serializers.EmailField(source="worker.email", read_only=True)
    job_title = serializers.CharField(source="job.title", read_only=True)
    assignment_id = serializers.SerializerMethodField()

    class Meta:
        model = JobApplication
        fields = ["id", "job", "job_title", "worker_email", "status", "cover_note", "applied_at", "reviewed_at", "assignment_id"]
        read_only_fields = ["id", "job", "job_title", "worker_email", "status", "applied_at", "reviewed_at", "assignment_id"]

    def get_assignment_id(self, obj):
        try:
            return str(obj.assignment.id)
        except Exception:
            return None
