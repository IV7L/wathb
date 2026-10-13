from django.db import transaction
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import Role
from accounts.permissions import IsWorker, IsBusiness
from assignments.models import (
    Assignment, AssignmentStatus, AttendanceRecord, AttendanceStatus,
    CompletionRecord, CompletionStatus,
)
from assignments.serializers import (
    AssignmentSerializer, AttendanceRecordSerializer, AttendanceSubmitSerializer,
    CompletionRecordSerializer, CompletionSubmitSerializer,
)
from jobs.models import JobApplication, ApplicationStatus, JobListingStatus


class AssignmentListView(generics.ListAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = AssignmentSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.WORKER:
            return Assignment.objects.filter(worker=user).select_related("job__business", "worker")
        elif user.role == Role.BUSINESS:
            return Assignment.objects.filter(job__business__user=user).select_related("job__business", "worker")
        elif user.role == Role.ADMIN:
            return Assignment.objects.all().select_related("job__business", "worker")
        return Assignment.objects.none()


class AssignmentDetailView(generics.RetrieveAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = AssignmentSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.WORKER:
            return Assignment.objects.filter(worker=user)
        elif user.role == Role.BUSINESS:
            return Assignment.objects.filter(job__business__user=user)
        elif user.role == Role.ADMIN:
            return Assignment.objects.all()
        return Assignment.objects.none()


class AssignmentAcceptView(APIView):
    permission_classes = [IsWorker]

    @transaction.atomic
    def post(self, request, pk):
        try:
            assignment = Assignment.objects.select_for_update().get(pk=pk, worker=request.user)
        except Assignment.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if assignment.status != AssignmentStatus.OFFERED:
            return Response(
                {"detail": f"Cannot accept an assignment with status '{assignment.status}'."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        snapshot = assignment.job.material_terms_snapshot()
        assignment.terms_snapshot = snapshot
        assignment.status = AssignmentStatus.ACCEPTED
        assignment.accepted_at = timezone.now()
        assignment.save(update_fields=["terms_snapshot", "status", "accepted_at"])

        application = assignment.application
        application.status = ApplicationStatus.ACCEPTED
        application.save(update_fields=["status"])

        job = assignment.job
        if job.status == JobListingStatus.PUBLISHED:
            job.status = JobListingStatus.FILLED
            job.save(update_fields=["status"])

        return Response(AssignmentSerializer(assignment).data)


class AssignmentDeclineView(APIView):
    permission_classes = [IsWorker]

    def post(self, request, pk):
        try:
            assignment = Assignment.objects.get(pk=pk, worker=request.user)
        except Assignment.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if assignment.status != AssignmentStatus.OFFERED:
            return Response(
                {"detail": f"Cannot decline an assignment with status '{assignment.status}'."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        assignment.status = AssignmentStatus.DECLINED
        assignment.save(update_fields=["status"])

        application = assignment.application
        application.status = ApplicationStatus.REJECTED
        application.save(update_fields=["status"])

        return Response(AssignmentSerializer(assignment).data)


class AssignmentCancelView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        user = request.user
        if user.role == Role.BUSINESS:
            qs = Assignment.objects.select_related("job").filter(pk=pk, job__business__user=user)
        elif user.role == Role.WORKER:
            qs = Assignment.objects.select_related("job").filter(pk=pk, worker=user)
        elif user.role == Role.ADMIN:
            qs = Assignment.objects.select_related("job").filter(pk=pk)
        else:
            return Response({"detail": "Forbidden."}, status=status.HTTP_403_FORBIDDEN)

        try:
            assignment = qs.get()
        except Assignment.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if assignment.status in (AssignmentStatus.COMPLETED, AssignmentStatus.CANCELLED):
            return Response({"detail": "Cannot cancel a completed or already-cancelled assignment."}, status=status.HTTP_400_BAD_REQUEST)

        if timezone.now().date() >= assignment.job.work_date:
            return Response({"detail": "Cannot cancel an assignment after the work date."}, status=status.HTTP_400_BAD_REQUEST)

        reason = request.data.get("reason", "").strip()
        if not reason:
            return Response({"detail": "reason is required."}, status=status.HTTP_400_BAD_REQUEST)

        assignment.status = AssignmentStatus.CANCELLED
        assignment.cancelled_at = timezone.now()
        assignment.cancellation_reason = reason
        assignment.save(update_fields=["status", "cancelled_at", "cancellation_reason"])

        if user.role == Role.BUSINESS:
            _notify(
                assignment.worker, "ASSIGNMENT_CANCELLED",
                f"تم إلغاء مهمتك: {assignment.job.title}",
                f"أُلغيت المهمة في {assignment.job.work_date}. السبب: {reason[:100]}",
                "Assignment", str(assignment.pk),
            )
        elif user.role == Role.WORKER:
            business_user = assignment.job.business.user
            _notify(
                business_user, "ASSIGNMENT_CANCELLED",
                f"ألغى العامل مهمته: {assignment.job.title}",
                f"ألغى العامل مشاركته في {assignment.job.work_date}. السبب: {reason[:100]}",
                "Assignment", str(assignment.pk),
            )

        return Response(AssignmentSerializer(assignment).data)


class AttendanceListCreateView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        assignment = self._get_assignment(request, pk)
        if assignment is None:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)
        records = AttendanceRecord.objects.filter(assignment=assignment)
        return Response(AttendanceRecordSerializer(records, many=True).data)

    def post(self, request, pk):
        assignment = self._get_assignment(request, pk)
        if assignment is None:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if assignment.status not in (AssignmentStatus.ACCEPTED, AssignmentStatus.ACTIVE):
            return Response({"detail": "Attendance can only be submitted for active assignments."}, status=status.HTTP_400_BAD_REQUEST)

        serializer = AttendanceSubmitSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        record = serializer.save(assignment=assignment, submitted_by=request.user)

        business_user = assignment.job.business.user
        _notify(
            business_user, "ATTENDANCE_SUBMITTED",
            f"تم تسجيل الحضور: {assignment.job.title}",
            f"سجّل العامل حضوره ليوم {record.work_date}. بانتظار تأكيدك.",
            "Assignment", str(assignment.pk),
        )

        return Response(AttendanceRecordSerializer(record).data, status=status.HTTP_201_CREATED)

    def _get_assignment(self, request, pk):
        user = request.user
        try:
            if user.role == Role.WORKER:
                return Assignment.objects.get(pk=pk, worker=user)
            elif user.role == Role.BUSINESS:
                return Assignment.objects.get(pk=pk, job__business__user=user)
            elif user.role == Role.ADMIN:
                return Assignment.objects.get(pk=pk)
        except Assignment.DoesNotExist:
            return None


class AttendanceConfirmView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk, rec_id):
        record, assignment = self._get_record_and_check_authority(request, pk, rec_id)
        if record is None:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if record.status != AttendanceStatus.SUBMITTED:
            return Response({"detail": "Only submitted attendance can be confirmed."}, status=status.HTTP_400_BAD_REQUEST)

        record.status = AttendanceStatus.CONFIRMED
        record.confirmed_by = request.user
        record.confirmed_at = timezone.now()
        record.save(update_fields=["status", "confirmed_by", "confirmed_at"])
        return Response(AttendanceRecordSerializer(record).data)

    def _get_record_and_check_authority(self, request, pk, rec_id):
        user = request.user
        try:
            if user.role == Role.WORKER:
                assignment = Assignment.objects.get(pk=pk, worker=user)
                record = AttendanceRecord.objects.get(pk=rec_id, assignment=assignment)
                if record.submitted_by == user:
                    return None, None
            elif user.role == Role.BUSINESS:
                assignment = Assignment.objects.get(pk=pk, job__business__user=user)
                record = AttendanceRecord.objects.get(pk=rec_id, assignment=assignment)
                if record.submitted_by.role == Role.BUSINESS:
                    return None, None
            elif user.role == Role.ADMIN:
                assignment = Assignment.objects.get(pk=pk)
                record = AttendanceRecord.objects.get(pk=rec_id, assignment=assignment)
            else:
                return None, None
            return record, assignment
        except (Assignment.DoesNotExist, AttendanceRecord.DoesNotExist):
            return None, None


class AttendanceDisputeView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk, rec_id):
        user = request.user
        try:
            if user.role == Role.WORKER:
                assignment = Assignment.objects.get(pk=pk, worker=user)
            elif user.role == Role.BUSINESS:
                assignment = Assignment.objects.get(pk=pk, job__business__user=user)
            else:
                return Response({"detail": "Forbidden."}, status=status.HTTP_403_FORBIDDEN)
            record = AttendanceRecord.objects.get(pk=rec_id, assignment=assignment)
        except (Assignment.DoesNotExist, AttendanceRecord.DoesNotExist):
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if record.status != AttendanceStatus.SUBMITTED:
            return Response({"detail": "Only submitted attendance can be disputed."}, status=status.HTTP_400_BAD_REQUEST)

        record.status = AttendanceStatus.DISPUTED
        record.save(update_fields=["status"])

        worker = assignment.worker
        _notify(
            worker, "ATTENDANCE_DISPUTED",
            f"اعتُرض على حضورك: {assignment.job.title}",
            f"اعترض صاحب العمل على سجل حضورك ليوم {record.work_date}.",
            "Assignment", str(assignment.pk),
        )

        return Response(AttendanceRecordSerializer(record).data)


class CompletionView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        assignment = self._get_assignment(request, pk)
        if assignment is None:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)
        try:
            record = assignment.completion_record
            return Response(CompletionRecordSerializer(record).data)
        except CompletionRecord.DoesNotExist:
            return Response({"detail": "No completion record yet."}, status=status.HTTP_404_NOT_FOUND)

    @transaction.atomic
    def post(self, request, pk):
        assignment = self._get_assignment(request, pk)
        if assignment is None:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if assignment.status not in (AssignmentStatus.ACCEPTED, AssignmentStatus.ACTIVE):
            return Response({"detail": "Completion can only be submitted for active assignments."}, status=status.HTTP_400_BAD_REQUEST)

        if CompletionRecord.objects.filter(assignment=assignment).exists():
            return Response({"detail": "Completion has already been submitted for this assignment."}, status=status.HTTP_409_CONFLICT)

        serializer = CompletionSubmitSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        record = serializer.save(assignment=assignment, submitted_by=request.user)

        assignment.status = AssignmentStatus.ACTIVE
        assignment.save(update_fields=["status"])

        business_user = assignment.job.business.user
        _notify(
            business_user, "COMPLETION_SUBMITTED",
            f"طلب إنهاء العمل: {assignment.job.title}",
            "أرسل العامل طلب تأكيد إنهاء العمل. بانتظار موافقتك.",
            "Assignment", str(assignment.pk),
        )

        return Response(CompletionRecordSerializer(record).data, status=status.HTTP_201_CREATED)

    def _get_assignment(self, request, pk):
        user = request.user
        try:
            if user.role == Role.WORKER:
                return Assignment.objects.get(pk=pk, worker=user)
            elif user.role == Role.BUSINESS:
                return Assignment.objects.get(pk=pk, job__business__user=user)
            elif user.role == Role.ADMIN:
                return Assignment.objects.get(pk=pk)
        except Assignment.DoesNotExist:
            return None


class CompletionConfirmView(APIView):
    permission_classes = [IsAuthenticated]

    @transaction.atomic
    def patch(self, request, pk):
        user = request.user
        try:
            if user.role == Role.WORKER:
                assignment = Assignment.objects.get(pk=pk, worker=user)
            elif user.role == Role.BUSINESS:
                assignment = Assignment.objects.get(pk=pk, job__business__user=user)
            elif user.role == Role.ADMIN:
                assignment = Assignment.objects.get(pk=pk)
            else:
                return Response({"detail": "Forbidden."}, status=status.HTTP_403_FORBIDDEN)
            record = assignment.completion_record
        except (Assignment.DoesNotExist, CompletionRecord.DoesNotExist):
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if record.status != CompletionStatus.SUBMITTED:
            return Response({"detail": "Only submitted completions can be confirmed."}, status=status.HTTP_400_BAD_REQUEST)

        if record.submitted_by == user and user.role != Role.ADMIN:
            return Response({"detail": "Cannot confirm your own completion submission."}, status=status.HTTP_400_BAD_REQUEST)

        record.status = CompletionStatus.CONFIRMED
        record.confirmed_by = user
        record.confirmed_at = timezone.now()
        record.save(update_fields=["status", "confirmed_by", "confirmed_at"])

        assignment.status = AssignmentStatus.COMPLETED
        assignment.save(update_fields=["status"])

        _create_wage_obligation(assignment)

        _notify(
            assignment.worker, "COMPLETION_CONFIRMED",
            f"تم تأكيد إنهاء العمل: {assignment.job.title}",
            "تم تأكيد إنهاء العمل. سيتم معالجة أجرك قريباً.",
            "Assignment", str(assignment.pk),
        )

        return Response(CompletionRecordSerializer(record).data)


class CompletionDisputeView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        user = request.user
        try:
            if user.role == Role.WORKER:
                assignment = Assignment.objects.get(pk=pk, worker=user)
            elif user.role == Role.BUSINESS:
                assignment = Assignment.objects.get(pk=pk, job__business__user=user)
            else:
                return Response({"detail": "Forbidden."}, status=status.HTTP_403_FORBIDDEN)
            record = assignment.completion_record
        except (Assignment.DoesNotExist, CompletionRecord.DoesNotExist):
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if record.status != CompletionStatus.SUBMITTED:
            return Response({"detail": "Only submitted completions can be disputed."}, status=status.HTTP_400_BAD_REQUEST)

        record.status = CompletionStatus.DISPUTED
        record.save(update_fields=["status"])

        _notify(
            assignment.worker, "COMPLETION_DISPUTED",
            f"اعتُرض على إنهاء العمل: {assignment.job.title}",
            "اعترض صاحب العمل على طلب إنهاء العمل. تواصل مع الدعم.",
            "Assignment", str(assignment.pk),
        )

        return Response(CompletionRecordSerializer(record).data)


def _notify(recipient, notification_type, title, body, related_model, related_id):
    try:
        from notifications.models import Notification
        Notification.objects.create(
            recipient=recipient,
            notification_type=notification_type,
            title=title,
            body=body,
            related_model=related_model,
            related_id=related_id,
        )
    except Exception:
        pass


def _create_wage_obligation(assignment):
    from django.db import IntegrityError
    from wages.models import WageObligation
    from datetime import date, timedelta

    if WageObligation.objects.filter(assignment=assignment).exists():
        return

    snapshot = assignment.terms_snapshot
    wage_amount = snapshot.get("wage_amount", 0)
    wage_currency = snapshot.get("wage_currency", "ILS")
    payment_deadline_days = snapshot.get("payment_deadline_days", 3)

    try:
        work_date = date.fromisoformat(snapshot.get("work_date", str(date.today())))
    except (ValueError, TypeError):
        work_date = date.today()

    due_date = work_date + timedelta(days=payment_deadline_days)

    try:
        WageObligation.objects.create(
            assignment=assignment,
            amount=wage_amount,
            currency=wage_currency,
            due_date=due_date,
        )
    except IntegrityError:
        pass
