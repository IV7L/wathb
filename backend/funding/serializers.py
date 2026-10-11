from rest_framework import serializers
from funding.models import FundingProposal, FundingBudget, FundingAllocation


class FundingBudgetSerializer(serializers.ModelSerializer):
    available_amount = serializers.IntegerField(read_only=True)

    class Meta:
        model = FundingBudget
        fields = [
            "id", "total_amount", "currency",
            "reserved_amount", "allocated_amount", "spent_amount", "available_amount",
        ]
        read_only_fields = fields


class FundingProposalSerializer(serializers.ModelSerializer):
    funder_email = serializers.EmailField(source="funder.email", read_only=True)
    budget = FundingBudgetSerializer(read_only=True)

    class Meta:
        model = FundingProposal
        fields = [
            "id", "funder_email", "title", "description",
            "total_amount", "currency", "status",
            "approved_at", "rejection_reason", "created_at", "budget",
        ]
        read_only_fields = ["id", "funder_email", "status", "approved_at", "rejection_reason", "created_at", "budget"]


class FundingProposalCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = FundingProposal
        fields = ["id", "title", "description", "total_amount", "currency"]
        read_only_fields = ["id"]

    def validate_total_amount(self, value):
        if value <= 0:
            raise serializers.ValidationError("Total amount must be positive.")
        return value


class FundingAllocationSerializer(serializers.ModelSerializer):
    class Meta:
        model = FundingAllocation
        fields = ["id", "budget", "job_listing", "amount", "currency", "status", "created_at"]
        read_only_fields = fields
