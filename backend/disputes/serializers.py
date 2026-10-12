from rest_framework import serializers
from disputes.models import Dispute, DisputeEvent


class DisputeEventSerializer(serializers.ModelSerializer):
    actor_phone = serializers.CharField(source="actor.phone", read_only=True)

    class Meta:
        model = DisputeEvent
        fields = ["id", "actor_phone", "action", "notes", "timestamp"]
        read_only_fields = fields


class DisputeSerializer(serializers.ModelSerializer):
    raised_by_phone = serializers.CharField(source="raised_by.phone", read_only=True)
    assigned_to_phone = serializers.CharField(source="assigned_to.phone", read_only=True, default=None)

    class Meta:
        model = Dispute
        fields = [
            "id", "category", "assignment", "wage_obligation",
            "raised_by_phone", "description", "status",
            "assigned_to_phone", "created_at", "resolved_at", "resolution_notes",
        ]
        read_only_fields = ["id", "raised_by_phone", "status", "assigned_to_phone", "created_at", "resolved_at", "resolution_notes"]


class DisputeCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Dispute
        fields = ["category", "assignment", "wage_obligation", "description"]

    def validate(self, attrs):
        user = self.context["request"].user
        assignment = attrs["assignment"]
        if assignment.worker != user and assignment.job.business.user != user:
            raise serializers.ValidationError("You are not a party to this assignment.")
        return attrs
