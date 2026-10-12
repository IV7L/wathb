from rest_framework import serializers
from assignments.models import Assignment, AttendanceRecord, CompletionRecord


class AssignmentSerializer(serializers.ModelSerializer):
    job_title = serializers.CharField(source="job.title", read_only=True)
    worker_phone = serializers.CharField(source="worker.phone", read_only=True)
    worker_name = serializers.SerializerMethodField()
    business_name = serializers.CharField(source="job.business.legal_name", read_only=True)
    # Normalized E.164 phone of the business user — ready for tel: and wa.me links.
    business_phone = serializers.CharField(source="job.business.user.phone", read_only=True)

    # Live job terms — shown on OFFERED cards so the worker can review before accepting.
    job_work_date = serializers.DateField(source="job.work_date", read_only=True)
    job_start_time = serializers.TimeField(source="job.start_time", read_only=True)
    job_end_time = serializers.TimeField(source="job.end_time", read_only=True)
    job_location_city = serializers.CharField(source="job.location_city", read_only=True)
    job_location_address = serializers.CharField(source="job.location_address", read_only=True)
    job_wage_amount = serializers.IntegerField(source="job.wage_amount", read_only=True)
    job_wage_currency = serializers.CharField(source="job.wage_currency", read_only=True)
    job_wage_basis = serializers.CharField(source="job.wage_basis", read_only=True)
    job_payment_method = serializers.CharField(source="job.payment_method", read_only=True)
    job_payment_deadline_days = serializers.IntegerField(source="job.payment_deadline_days", read_only=True)

    def get_worker_name(self, obj):
        try:
            return obj.worker.worker_profile.full_name
        except Exception:
            return None

    class Meta:
        model = Assignment
        fields = [
            "id", "job", "job_title", "worker_phone", "worker_name", "business_name", "business_phone",
            "job_work_date", "job_start_time", "job_end_time",
            "job_location_city", "job_location_address",
            "job_wage_amount", "job_wage_currency", "job_wage_basis",
            "job_payment_method", "job_payment_deadline_days",
            "terms_snapshot", "status", "offered_at", "accepted_at",
            "cancelled_at", "cancellation_reason",
        ]
        read_only_fields = fields


class AttendanceRecordSerializer(serializers.ModelSerializer):
    submitted_by_phone = serializers.CharField(source="submitted_by.phone", read_only=True)

    class Meta:
        model = AttendanceRecord
        fields = [
            "id", "assignment", "submitted_by_phone", "submitted_at",
            "work_date", "check_in_time", "check_out_time", "notes",
            "status", "confirmed_at",
        ]
        read_only_fields = ["id", "assignment", "submitted_by_phone", "submitted_at", "status", "confirmed_at"]


class AttendanceSubmitSerializer(serializers.ModelSerializer):
    class Meta:
        model = AttendanceRecord
        fields = ["work_date", "check_in_time", "check_out_time", "notes"]


class CompletionRecordSerializer(serializers.ModelSerializer):
    submitted_by_phone = serializers.CharField(source="submitted_by.phone", read_only=True)

    class Meta:
        model = CompletionRecord
        fields = [
            "id", "assignment", "submitted_by_phone", "submitted_at",
            "hours_worked", "notes", "status", "confirmed_at",
        ]
        read_only_fields = ["id", "assignment", "submitted_by_phone", "submitted_at", "status", "confirmed_at"]


class CompletionSubmitSerializer(serializers.ModelSerializer):
    class Meta:
        model = CompletionRecord
        fields = ["hours_worked", "notes"]
