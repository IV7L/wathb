from rest_framework import serializers
from disputes.models import Dispute, DisputeEvent


class DisputeEventSerializer(serializers.ModelSerializer):
    actor_email = serializers.EmailField(source="actor.email", read_only=True)

    class Meta:
        model = DisputeEvent
        fields = ["id", "actor_email", "action", "notes", "timestamp"]
        read_only_fields = fields


class DisputeSerializer(serializers.ModelSerializer):
    raised_by_email = serializers.EmailField(source="raised_by.email", read_only=True)
    assigned_to_email = serializers.EmailField(source="assigned_to.email", read_only=True, default=None)

    class Meta:
        model = Dispute
        fields = [
            "id", "category", "assignment", "wage_obligation",
            "raised_by_email", "description", "status",
            "assigned_to_email", "created_at", "resolved_at", "resolution_notes",
        ]
        read_only_fields = ["id", "raised_by_email", "status", "assigned_to_email", "created_at", "resolved_at", "resolution_notes"]


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
