from django.db.models import Q
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import Role
from accounts.permissions import IsAdminRole
from disputes.models import Dispute, DisputeEvent, DisputeStatus
from disputes.serializers import DisputeSerializer, DisputeCreateSerializer, DisputeEventSerializer


class DisputeListCreateView(generics.ListCreateAPIView):
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == "POST":
            return DisputeCreateSerializer
        return DisputeSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.ADMIN:
            return Dispute.objects.all().select_related("raised_by", "assigned_to")
        if user.role == Role.BUSINESS:
            return Dispute.objects.filter(
                Q(raised_by=user) | Q(assignment__job__business__user=user)
            ).distinct().select_related("raised_by", "assigned_to")
        return Dispute.objects.filter(raised_by=user).select_related("raised_by", "assigned_to")

    def perform_create(self, serializer):
        dispute = serializer.save(raised_by=self.request.user)
        DisputeEvent.objects.create(
            dispute=dispute,
            actor=self.request.user,
            action="opened",
            notes=dispute.description[:200],
        )


class DisputeDetailView(generics.RetrieveAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = DisputeSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.ADMIN:
            return Dispute.objects.all()
        if user.role == Role.BUSINESS:
            return Dispute.objects.filter(
                Q(raised_by=user) | Q(assignment__job__business__user=user)
            ).distinct()
        return Dispute.objects.filter(raised_by=user)


class DisputeEventListView(generics.ListAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = DisputeEventSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.ADMIN:
            return DisputeEvent.objects.filter(dispute__pk=self.kwargs["pk"])
        if user.role == Role.BUSINESS:
            return DisputeEvent.objects.filter(
                dispute__pk=self.kwargs["pk"]
            ).filter(
                Q(dispute__raised_by=user) | Q(dispute__assignment__job__business__user=user)
            ).distinct()
        return DisputeEvent.objects.filter(dispute__pk=self.kwargs["pk"], dispute__raised_by=user)


class AdminDisputeAssignView(APIView):
    permission_classes = [IsAdminRole]

    def post(self, request, pk):
        try:
            dispute = Dispute.objects.get(pk=pk)
        except Dispute.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if dispute.status not in (DisputeStatus.OPEN,):
            return Response({"detail": "Can only assign an open dispute."}, status=status.HTTP_400_BAD_REQUEST)

        dispute.assigned_to = request.user
        dispute.status = DisputeStatus.IN_REVIEW
        dispute.save(update_fields=["assigned_to", "status"])

        DisputeEvent.objects.create(
            dispute=dispute,
            actor=request.user,
            action="assigned",
            notes=f"Assigned to {request.user.email}",
        )
        return Response(DisputeSerializer(dispute).data)


class AdminDisputeResolveView(APIView):
    permission_classes = [IsAdminRole]

    def post(self, request, pk):
        try:
            dispute = Dispute.objects.get(pk=pk)
        except Dispute.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if dispute.status not in (DisputeStatus.OPEN, DisputeStatus.IN_REVIEW):
            return Response({"detail": "Can only resolve open or in-review disputes."}, status=status.HTTP_400_BAD_REQUEST)

        resolution_notes = request.data.get("resolution_notes", "")
        if not resolution_notes:
            return Response({"detail": "resolution_notes is required."}, status=status.HTTP_400_BAD_REQUEST)

        dispute.status = DisputeStatus.RESOLVED
        dispute.resolved_at = timezone.now()
        dispute.resolution_notes = resolution_notes
        dispute.save(update_fields=["status", "resolved_at", "resolution_notes"])

        DisputeEvent.objects.create(
            dispute=dispute,
            actor=request.user,
            action="resolved",
            notes=resolution_notes[:500],
        )

        _log_audit(request.user, "dispute_resolved", "Dispute", str(dispute.pk), {"notes": resolution_notes[:200]})
        _notify_dispute_resolved(dispute, resolution_notes)
        return Response(DisputeSerializer(dispute).data)


def _notify_dispute_resolved(dispute, notes):
    try:
        from notifications.models import Notification
        Notification.objects.create(
            recipient=dispute.raised_by,
            notification_type="DISPUTE_STATUS_CHANGED",
            title="تم حل النزاع",
            body=f"تم حل نزاعك. ملاحظات المسؤول: {notes[:100]}",
            related_model="Dispute",
            related_id=str(dispute.pk),
        )
    except Exception:
        pass


def _log_audit(actor, action, model_name, object_id, details=None):
    try:
        from audit.models import AuditEvent
        AuditEvent.objects.create(
            actor=actor,
            action=action,
            model_name=model_name,
            object_id=object_id,
            details=details or {},
        )
    except Exception:
        pass
