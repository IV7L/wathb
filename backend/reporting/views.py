import csv
from django.http import HttpResponse
from rest_framework.response import Response
from rest_framework.views import APIView
from accounts.permissions import IsAdminRole
from accounts.models import BusinessProfile, VerificationStatus
from assignments.models import Assignment, AssignmentStatus
from wages.models import WageObligation, WageObligationStatus
from disputes.models import Dispute, DisputeStatus
from jobs.models import JobListing, JobListingStatus, JobApplication


class PilotSummaryView(APIView):
    permission_classes = [IsAdminRole]

    def get(self, request):
        data = _compute_pilot_summary()
        return Response(data)


class PilotSummaryExportView(APIView):
    permission_classes = [IsAdminRole]

    def get(self, request):
        data = _compute_pilot_summary()
        response = HttpResponse(content_type="text/csv")
        response["Content-Disposition"] = 'attachment; filename="pilot_summary.csv"'
        writer = csv.writer(response)
        writer.writerow(["Metric", "Value"])
        for key, value in data.items():
            writer.writerow([key, value])
        return response


def _compute_pilot_summary():
    verified_businesses = BusinessProfile.objects.filter(
        verification_status=VerificationStatus.APPROVED
    ).count()

    from accounts.models import User, Role
    verified_workers = User.objects.filter(role=Role.WORKER, is_verified=True).count()

    published_jobs = JobListing.objects.exclude(status=JobListingStatus.DRAFT).count()
    applications = JobApplication.objects.count()
    accepted_assignments = Assignment.objects.exclude(
        status__in=[AssignmentStatus.DECLINED, AssignmentStatus.CANCELLED]
    ).count()
    completed_assignments = Assignment.objects.filter(status=AssignmentStatus.COMPLETED).count()

    wages_created = WageObligation.objects.count()
    payments_declared = WageObligation.objects.filter(
        status__in=[
            WageObligationStatus.DECLARED,
            WageObligationStatus.AWAITING_ACK,
            WageObligationStatus.UNDER_REVIEW,
            WageObligationStatus.VERIFIED_PAID,
        ]
    ).count()
    payments_verified = WageObligation.objects.filter(
        status=WageObligationStatus.VERIFIED_PAID
    ).count()

    total_verified_paid = sum(
        WageObligation.objects.filter(status=WageObligationStatus.VERIFIED_PAID).values_list("amount", flat=True)
    )

    disputes_opened = Dispute.objects.count()
    disputes_resolved = Dispute.objects.filter(status=DisputeStatus.RESOLVED).count()

    return {
        "verified_businesses": verified_businesses,
        "verified_workers": verified_workers,
        "published_jobs": published_jobs,
        "applications": applications,
        "accepted_assignments": accepted_assignments,
        "completed_worker_days": completed_assignments,
        "wage_obligations_created": wages_created,
        "payments_declared": payments_declared,
        "payments_verified_paid": payments_verified,
        "total_verified_paid_minor_units": total_verified_paid,
        "disputes_opened": disputes_opened,
        "disputes_resolved": disputes_resolved,
    }
