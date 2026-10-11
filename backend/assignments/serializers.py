from rest_framework import serializers
from assignments.models import Assignment, AttendanceRecord, CompletionRecord


class AssignmentSerializer(serializers.ModelSerializer):
    job_title = serializers.CharField(source="job.title", read_only=True)
    worker_email = serializers.EmailField(source="worker.email", read_only=True)
    business_name = serializers.CharField(source="job.business.legal_name", read_only=True)

    class Meta:
        model = Assignment
        fields = [
            "id", "job", "job_title", "worker_email", "business_name",
            "terms_snapshot", "status", "offered_at", "accepted_at",
            "cancelled_at", "cancellation_reason",
        ]
        read_only_fields = fields


class AttendanceRecordSerializer(serializers.ModelSerializer):
    submitted_by_email = serializers.EmailField(source="submitted_by.email", read_only=True)

    class Meta:
        model = AttendanceRecord
        fields = [
            "id", "assignment", "submitted_by_email", "submitted_at",
            "work_date", "check_in_time", "check_out_time", "notes",
            "status", "confirmed_at",
        ]
        read_only_fields = ["id", "assignment", "submitted_by_email", "submitted_at", "status", "confirmed_at"]


class AttendanceSubmitSerializer(serializers.ModelSerializer):
    class Meta:
        model = AttendanceRecord
        fields = ["work_date", "check_in_time", "check_out_time", "notes"]


class CompletionRecordSerializer(serializers.ModelSerializer):
    submitted_by_email = serializers.EmailField(source="submitted_by.email", read_only=True)

    class Meta:
        model = CompletionRecord
        fields = [
            "id", "assignment", "submitted_by_email", "submitted_at",
            "hours_worked", "notes", "status", "confirmed_at",
        ]
        read_only_fields = ["id", "assignment", "submitted_by_email", "submitted_at", "status", "confirmed_at"]


class CompletionSubmitSerializer(serializers.ModelSerializer):
    class Meta:
        model = CompletionRecord
        fields = ["hours_worked", "notes"]
