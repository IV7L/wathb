import datetime
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from notifications.models import Notification, NotificationType
from notifications.serializers import NotificationSerializer


def _create_wage_reminders(user):
    """Lazily create wage reminder notifications for workers with stale AWAITING_ACK obligations."""
    from accounts.models import Role
    if user.role != Role.WORKER:
        return
    from django.utils import timezone
    from wages.models import WageObligation, WageObligationStatus, PaymentDeclaration, PaymentDeclarationStatus
    now = timezone.now()
    cutoff_1d = now - datetime.timedelta(days=1)
    cutoff_3d = now - datetime.timedelta(days=3)
    obligations = WageObligation.objects.filter(
        assignment__worker=user,
        status=WageObligationStatus.AWAITING_ACK,
        declarations__status=PaymentDeclarationStatus.SUBMITTED,
    ).distinct().prefetch_related("declarations")
    for obl in obligations:
        declared_at = obl.declarations.filter(
            status=PaymentDeclarationStatus.SUBMITTED
        ).order_by("declared_at").values_list("declared_at", flat=True).first()
        if declared_at is None:
            continue
        if declared_at <= cutoff_3d:
            if not Notification.objects.filter(
                recipient=user,
                notification_type=NotificationType.WAGE_REMINDER_3D,
                related_model="WageObligation",
                related_id=str(obl.pk),
            ).exists():
                Notification.objects.create(
                    recipient=user,
                    notification_type=NotificationType.WAGE_REMINDER_3D,
                    title="تذكير: لم تُقرّ استلام أجرك منذ 3 أيام",
                    body="الرجاء مراجعة أجرك وتأكيد استلامه أو الاعتراض عليه.",
                    related_model="WageObligation",
                    related_id=str(obl.pk),
                )
        elif declared_at <= cutoff_1d:
            if not Notification.objects.filter(
                recipient=user,
                notification_type=NotificationType.WAGE_REMINDER_1D,
                related_model="WageObligation",
                related_id=str(obl.pk),
            ).exists():
                Notification.objects.create(
                    recipient=user,
                    notification_type=NotificationType.WAGE_REMINDER_1D,
                    title="تذكير: لم تُقرّ استلام أجرك بعد",
                    body="مرّ يوم على إعلان الدفع. الرجاء تأكيد الاستلام أو الاعتراض.",
                    related_model="WageObligation",
                    related_id=str(obl.pk),
                )


def _auto_confirm_all_pending():
    from assignments.views import _auto_confirm_pending
    from assignments.models import Assignment, AttendanceRecord, AttendanceStatus, CompletionRecord, CompletionStatus
    from django.utils import timezone as tz
    now = tz.now()
    cutoff = now - datetime.timedelta(hours=24)
    pks = set()
    for pk in AttendanceRecord.objects.filter(status=AttendanceStatus.SUBMITTED, submitted_at__lte=cutoff).values_list("assignment_id", flat=True):
        pks.add(pk)
    for pk in CompletionRecord.objects.filter(status=CompletionStatus.SUBMITTED, submitted_at__lte=cutoff).values_list("assignment_id", flat=True):
        pks.add(pk)
    for assignment in Assignment.objects.filter(pk__in=pks).select_related("job__business", "worker"):
        _auto_confirm_pending(assignment)


class NotificationListView(generics.ListAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = NotificationSerializer

    def get_queryset(self):
        return Notification.objects.filter(recipient=self.request.user)

    def list(self, request, *args, **kwargs):
        _auto_confirm_all_pending()
        _create_wage_reminders(request.user)
        return super().list(request, *args, **kwargs)


class NotificationMarkReadView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        try:
            n = Notification.objects.get(pk=pk, recipient=request.user)
        except Notification.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)
        n.is_read = True
        n.save(update_fields=["is_read"])
        return Response(NotificationSerializer(n).data)


class NotificationMarkAllReadView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        Notification.objects.filter(recipient=request.user, is_read=False).update(is_read=True)
        return Response({"detail": "All notifications marked as read."})
