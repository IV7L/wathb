from django.db.models import Count, F, Q
from django.utils import timezone
from rest_framework import serializers
from jobs.models import JobListing, JobApplication, JobCategory


class JobListingSerializer(serializers.ModelSerializer):
    business_name = serializers.CharField(source="business.legal_name", read_only=True)
    business_verification_status = serializers.CharField(source="business.verification_status", read_only=True)
    business_track_record = serializers.SerializerMethodField()
    wage_display = serializers.SerializerMethodField()
    is_new = serializers.SerializerMethodField()

    class Meta:
        model = JobListing
        fields = [
            "id", "business_name", "business_verification_status", "business_track_record",
            "category",
            "title", "description", "location_address", "location_city",
            "work_date", "start_time", "end_time", "num_workers_needed",
            "required_skills", "wage_amount", "wage_currency", "wage_basis",
            "wage_display", "payment_method", "payment_deadline_days",
            "attendance_requirements", "cancellation_policy", "additional_conditions",
            "thumbnail_url",
            "status", "is_funded", "is_new", "published_at", "version", "created_at",
        ]
        read_only_fields = ["id", "business_name", "business_verification_status",
                            "business_track_record", "status", "is_new", "published_at", "version", "created_at"]

    def get_is_new(self, obj):
        if not obj.published_at:
            return False
        return (timezone.now() - obj.published_at).total_seconds() < 86400

    def get_wage_display(self, obj):
        major = obj.wage_amount / 100
        return f"{major:.0f} {obj.wage_currency} / {obj.get_wage_basis_display()}"

    def get_business_track_record(self, obj):
        from wages.models import WageObligation, WageObligationStatus
        agg = WageObligation.objects.filter(
            assignment__job__business=obj.business,
            status=WageObligationStatus.VERIFIED_PAID,
        ).aggregate(
            total_paid=Count("id"),
            on_time=Count("id", filter=Q(updated_at__date__lte=F("due_date"))),
        )
        total = agg["total_paid"]
        if total == 0:
            return {"label": "منشأة جديدة", "total_paid_days": 0, "on_time_days": 0}
        return {
            "label": f"دفعت {total} يوم · {agg['on_time']} في الوقت",
            "total_paid_days": total,
            "on_time_days": agg["on_time"],
        }


class JobListingCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = JobListing
        fields = [
            "category",
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
    worker_email = serializers.EmailField(source="worker.email", read_only=True, allow_null=True, default=None)
    worker_name = serializers.SerializerMethodField()
    worker_phone = serializers.CharField(source="worker.phone", read_only=True)
    worker_is_verified = serializers.BooleanField(source="worker.is_verified", read_only=True)
    worker_reliability = serializers.SerializerMethodField()
    job_title = serializers.CharField(source="job.title", read_only=True)
    assignment_id = serializers.SerializerMethodField()

    class Meta:
        model = JobApplication
        fields = [
            "id", "job", "job_title",
            "worker_email", "worker_name", "worker_phone", "worker_is_verified",
            "worker_reliability",
            "status", "cover_note", "applied_at", "reviewed_at", "assignment_id",
        ]
        read_only_fields = [
            "id", "job", "job_title",
            "worker_email", "worker_name", "worker_phone", "worker_is_verified",
            "worker_reliability",
            "status", "applied_at", "reviewed_at", "assignment_id",
        ]

    def get_worker_name(self, obj):
        try:
            return obj.worker.worker_profile.full_name
        except Exception:
            return None

    def get_assignment_id(self, obj):
        try:
            return str(obj.assignment.id)
        except Exception:
            return None

    def get_worker_reliability(self, obj):
        from assignments.models import AttendanceRecord, AttendanceStatus
        month_start = timezone.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        attended = AttendanceRecord.objects.filter(
            assignment__worker=obj.worker,
            status=AttendanceStatus.CONFIRMED,
            work_date__gte=month_start.date(),
        ).count()
        no_show = AttendanceRecord.objects.filter(
            assignment__worker=obj.worker,
            status=AttendanceStatus.DISPUTED,
            work_date__gte=month_start.date(),
        ).count()
        if attended == 0 and no_show == 0:
            return {"label": "جديد", "attended": 0, "no_show": 0}
        return {
            "label": f"حضر {attended} · غاب {no_show} (هذا الشهر)",
            "attended": attended,
            "no_show": no_show,
        }
