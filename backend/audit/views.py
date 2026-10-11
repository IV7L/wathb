from rest_framework import generics
from accounts.permissions import IsAdminRole
from audit.models import AuditEvent
from audit.serializers import AuditEventSerializer


class AuditLogView(generics.ListAPIView):
    permission_classes = [IsAdminRole]
    serializer_class = AuditEventSerializer
    filterset_fields = ["action", "model_name"]
    search_fields = ["action", "model_name", "object_id"]

    def get_queryset(self):
        return AuditEvent.objects.select_related("actor").all()
