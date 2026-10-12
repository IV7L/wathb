from rest_framework import serializers
from audit.models import AuditEvent


class AuditEventSerializer(serializers.ModelSerializer):
    actor_phone = serializers.CharField(source="actor.phone", read_only=True, default=None)

    class Meta:
        model = AuditEvent
        fields = ["id", "actor_phone", "action", "model_name", "object_id", "details", "timestamp"]
        read_only_fields = fields
