from django.db import transaction
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import Role
from accounts.permissions import IsWorker, IsBusiness, IsVerifiedBusiness
from jobs.models import JobListing, JobApplication, JobListingStatus, ApplicationStatus, JobCategory
from jobs.serializers import (
    JobListingSerializer,
    JobListingCreateSerializer,
    JobApplicationSerializer,
)


class JobListView(generics.ListCreateAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = JobListingSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.WORKER:
            qs = JobListing.objects.filter(status=JobListingStatus.PUBLISHED).select_related("business")
        elif user.role == Role.BUSINESS:
            qs = JobListing.objects.filter(business__user=user).select_related("business")
        elif user.role == Role.ADMIN:
            qs = JobListing.objects.all().select_related("business")
        else:
            qs = JobListing.objects.none()

        city = self.request.query_params.get("city")
        if city:
            qs = qs.filter(location_city__icontains=city)
        date = self.request.query_params.get("work_date")
        if date:
            qs = qs.filter(work_date=date)
        category = self.request.query_params.get("category")
        if category:
            qs = qs.filter(category=category)
        return qs

    def get_permissions(self):
        if self.request.method == "POST":
            return [IsVerifiedBusiness()]
        return [IsAuthenticated()]

    def create(self, request, *args, **kwargs):
        # Validate with the write serializer, respond with the full read serializer
        # so the client receives id, status, and all computed fields.
        write_ser = JobListingCreateSerializer(data=request.data)
        write_ser.is_valid(raise_exception=True)
        instance = write_ser.save(business=request.user.business_profile)
        return Response(JobListingSerializer(instance).data, status=status.HTTP_201_CREATED)


class JobDetailView(generics.RetrieveUpdateDestroyAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = JobListingSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.BUSINESS:
            return JobListing.objects.filter(business__user=user)
        if user.role == Role.ADMIN:
            return JobListing.objects.all()
        return JobListing.objects.filter(status=JobListingStatus.PUBLISHED)

    def update(self, request, *args, **kwargs):
        partial = kwargs.pop("partial", False)
        instance = self.get_object()
        write_ser = JobListingCreateSerializer(instance, data=request.data, partial=partial)
        write_ser.is_valid(raise_exception=True)
        instance = write_ser.save(version=instance.version + 1)
        return Response(JobListingSerializer(instance).data)

    def perform_destroy(self, instance):
        if instance.status != JobListingStatus.DRAFT:
            from rest_framework.exceptions import ValidationError
            raise ValidationError("Only draft listings can be deleted.")
        instance.delete()


class JobSubmitView(APIView):
    """Move a DRAFT listing to PENDING_REVIEW for publishing."""
    permission_classes = [IsVerifiedBusiness]

    def post(self, request, pk):
        try:
            job = JobListing.objects.get(pk=pk, business__user=request.user)
        except JobListing.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if not job.can_transition_to(JobListingStatus.PENDING_REVIEW):
            return Response(
                {"detail": f"Cannot submit a listing with status '{job.status}'."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        job.status = JobListingStatus.PENDING_REVIEW
        job.save(update_fields=["status"])
        return Response(JobListingSerializer(job).data)


class JobPublishView(APIView):
    permission_classes = [IsVerifiedBusiness]

    def post(self, request, pk):
        try:
            job = JobListing.objects.get(pk=pk, business__user=request.user)
        except JobListing.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if not job.can_transition_to(JobListingStatus.PUBLISHED):
            return Response(
                {"detail": f"Cannot publish a listing with status '{job.status}'."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if job.is_funded:
            from funding.models import FundingAllocation
            has_allocation = FundingAllocation.objects.filter(
                job_listing=job, status__in=["RESERVED", "ALLOCATED"]
            ).exists()
            if not has_allocation:
                return Response(
                    {"detail": "Funded job requires an approved funding allocation before publishing."},
                    status=status.HTTP_400_BAD_REQUEST,
                )

        job.status = JobListingStatus.PUBLISHED
        job.published_at = timezone.now()
        job.save(update_fields=["status", "published_at"])
        return Response(JobListingSerializer(job).data)


class JobCancelView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        user = request.user
        if user.role == Role.BUSINESS:
            qs = JobListing.objects.filter(pk=pk, business__user=user)
        elif user.role == Role.ADMIN:
            qs = JobListing.objects.filter(pk=pk)
        else:
            return Response({"detail": "Forbidden."}, status=status.HTTP_403_FORBIDDEN)

        try:
            job = qs.get()
        except JobListing.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if not job.can_transition_to(JobListingStatus.CANCELLED):
            return Response(
                {"detail": f"Cannot cancel a listing with status '{job.status}'."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        reason = request.data.get("reason", "").strip()
        if not reason:
            return Response({"detail": "reason is required."}, status=status.HTTP_400_BAD_REQUEST)

        with transaction.atomic():
            job.status = JobListingStatus.CANCELLED
            job.cancellation_reason = reason
            job.save(update_fields=["status", "cancellation_reason"])

            # Capture workers to notify before bulk update changes statuses.
            workers_to_notify = list(
                job.applications
                .filter(status__in=[ApplicationStatus.PENDING, ApplicationStatus.OFFERED])
                .select_related("worker")
                .values_list("worker", flat=True)
            )

            # Cancel pending/offered applications.
            job.applications.filter(
                status__in=[ApplicationStatus.PENDING, ApplicationStatus.OFFERED]
            ).update(status=ApplicationStatus.CANCELLED)

            # Cancel accepted assignments (work hasn't started yet).
            from assignments.models import Assignment, AssignmentStatus
            Assignment.objects.filter(
                job=job,
                status__in=[AssignmentStatus.OFFERED, AssignmentStatus.ACCEPTED]
            ).update(status=AssignmentStatus.CANCELLED)

            # Notify affected workers.
            from accounts.models import User as AccountUser
            for worker in AccountUser.objects.filter(pk__in=workers_to_notify):
                _notify_worker(worker, "JOB_CANCELLED", job)

        return Response(JobListingSerializer(job).data)


class JobCloneView(APIView):
    """Clone an existing job listing as a new DRAFT owned by the same business."""
    permission_classes = [IsVerifiedBusiness]

    def post(self, request, pk):
        try:
            original = JobListing.objects.get(pk=pk, business__user=request.user)
        except JobListing.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        clone = JobListing(
            business=original.business,
            title=original.title,
            description=original.description,
            location_address=original.location_address,
            location_city=original.location_city,
            work_date=original.work_date,
            start_time=original.start_time,
            end_time=original.end_time,
            num_workers_needed=original.num_workers_needed,
            wage_amount=original.wage_amount,
            wage_currency=original.wage_currency,
            wage_basis=original.wage_basis,
            payment_method=original.payment_method,
            payment_deadline_days=original.payment_deadline_days,
            required_skills=original.required_skills,
            attendance_requirements=original.attendance_requirements,
            cancellation_policy=original.cancellation_policy,
            additional_conditions=original.additional_conditions,
            is_funded=original.is_funded,
            status=JobListingStatus.DRAFT,
        )
        clone.save()
        return Response(JobListingSerializer(clone).data, status=status.HTTP_201_CREATED)


class JobCategoryListView(APIView):
    """Public-ish list of job categories (no auth required — workers see chips before login)."""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response([
            {"value": value, "label": label}
            for value, label in JobCategory.choices
        ])


class JobApplicationListView(generics.ListAPIView):
    permission_classes = [IsBusiness]
    serializer_class = JobApplicationSerializer

    def get_queryset(self):
        from django.db.models import Count, Q
        from django.utils import timezone
        try:
            job = JobListing.objects.get(pk=self.kwargs["pk"], business__user=self.request.user)
        except JobListing.DoesNotExist:
            return JobApplication.objects.none()
        month_start = timezone.now().replace(day=1, hour=0, minute=0, second=0, microsecond=0)
        return (
            JobApplication.objects
            .filter(job=job)
            .select_related("worker", "worker__worker_profile")
            .annotate(
                attended_this_month=Count(
                    "worker__assignments__attendance_records",
                    filter=Q(
                        worker__assignments__attendance_records__status="CONFIRMED",
                        worker__assignments__attendance_records__work_date__gte=month_start.date(),
                    ),
                    distinct=True,
                )
            )
            .order_by("attended_this_month", "applied_at")
        )


class WorkerApplicationListView(generics.ListAPIView):
    """Worker's own applications across all jobs."""
    permission_classes = [IsWorker]
    serializer_class = JobApplicationSerializer

    def get_queryset(self):
        return (
            JobApplication.objects
            .filter(worker=self.request.user)
            .select_related("worker", "job")
            .order_by("-applied_at")
        )


class JobApplyView(APIView):
    permission_classes = [IsWorker]

    def post(self, request, pk):
        try:
            job = JobListing.objects.get(pk=pk, status=JobListingStatus.PUBLISHED)
        except JobListing.DoesNotExist:
            return Response({"detail": "Job not found or not available."}, status=status.HTTP_404_NOT_FOUND)

        if JobApplication.objects.filter(job=job, worker=request.user).exists():
            return Response({"detail": "You have already applied to this job."}, status=status.HTTP_409_CONFLICT)

        app = JobApplication.objects.create(
            job=job,
            worker=request.user,
            cover_note=request.data.get("cover_note", ""),
        )
        return Response(JobApplicationSerializer(app).data, status=status.HTTP_201_CREATED)


class ApplicationActionView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk, app_id):
        user = request.user

        if user.role == Role.BUSINESS:
            try:
                job = JobListing.objects.get(pk=pk, business__user=user)
                app = JobApplication.objects.get(pk=app_id, job=job)
            except (JobListing.DoesNotExist, JobApplication.DoesNotExist):
                return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

            action_name = request.data.get("action")
            if action_name == "offer":
                if app.status != ApplicationStatus.PENDING:
                    return Response({"detail": "Application is not pending."}, status=status.HTTP_400_BAD_REQUEST)
                with transaction.atomic():
                    app.status = ApplicationStatus.OFFERED
                    app.reviewed_at = timezone.now()
                    app.save(update_fields=["status", "reviewed_at"])
                    from assignments.models import Assignment, AssignmentStatus
                    assignment, created = Assignment.objects.get_or_create(
                        job=job,
                        worker=app.worker,
                        defaults={
                            "application": app,
                            "terms_snapshot": {},
                            "status": AssignmentStatus.OFFERED,
                        },
                    )
                    if not created:
                        assignment.status = AssignmentStatus.OFFERED
                        assignment.save(update_fields=["status"])
                _notify_worker(app.worker, "JOB_OFFER", job)
            elif action_name == "reject":
                if app.status not in (ApplicationStatus.PENDING, ApplicationStatus.OFFERED):
                    return Response({"detail": "Cannot reject at this stage."}, status=status.HTTP_400_BAD_REQUEST)
                app.status = ApplicationStatus.REJECTED
                app.reviewed_at = timezone.now()
                app.save(update_fields=["status", "reviewed_at"])
                _notify_worker(app.worker, "APPLICATION_REJECTED", job)
            else:
                return Response({"detail": "action must be offer or reject."}, status=status.HTTP_400_BAD_REQUEST)

            return Response(JobApplicationSerializer(app).data)

        elif user.role == Role.WORKER:
            try:
                app = JobApplication.objects.get(pk=app_id, job__pk=pk, worker=user)
            except JobApplication.DoesNotExist:
                return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

            action_name = request.data.get("action")
            if action_name == "withdraw":
                if app.status not in (ApplicationStatus.PENDING, ApplicationStatus.OFFERED):
                    return Response({"detail": "Cannot withdraw at this stage."}, status=status.HTTP_400_BAD_REQUEST)
                app.status = ApplicationStatus.WITHDRAWN
                app.save(update_fields=["status"])
                return Response(JobApplicationSerializer(app).data)
            else:
                return Response({"detail": "Workers may only withdraw."}, status=status.HTTP_400_BAD_REQUEST)

        return Response({"detail": "Forbidden."}, status=status.HTTP_403_FORBIDDEN)

    def delete(self, request, pk, app_id):
        try:
            app = JobApplication.objects.get(pk=app_id, job__pk=pk, worker=request.user)
        except JobApplication.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if app.status not in (ApplicationStatus.PENDING,):
            return Response({"detail": "Cannot delete at this stage."}, status=status.HTTP_400_BAD_REQUEST)
        app.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


def _notify_worker(worker, notification_type, job):
    try:
        from notifications.models import Notification
        titles = {
            "JOB_OFFER": f"تم عرض وظيفة عليك: {job.title}",
            "APPLICATION_REJECTED": f"تم رفض طلبك: {job.title}",
            "JOB_CANCELLED": f"تم إلغاء الوظيفة: {job.title}",
        }
        bodies = {
            "JOB_OFFER": f"عرضت عليك {job.business.legal_name} العمل في '{job.title}' بتاريخ {job.work_date}.",
            "APPLICATION_REJECTED": f"تم رفض طلبك للوظيفة '{job.title}'.",
            "JOB_CANCELLED": f"أُلغيت الوظيفة '{job.title}' بتاريخ {job.work_date}.",
        }
        Notification.objects.create(
            recipient=worker,
            notification_type=notification_type,
            title=titles.get(notification_type, ""),
            body=bodies.get(notification_type, ""),
            related_model="JobListing",
            related_id=str(job.id),
        )
    except Exception:
        pass
