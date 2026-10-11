from django.db import transaction
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from accounts.models import Role
from accounts.permissions import IsFunder, IsAdminRole
from funding.models import FundingProposal, FundingBudget, FundingProposalStatus
from funding.serializers import (
    FundingProposalSerializer, FundingProposalCreateSerializer, FundingBudgetSerializer,
)


class FundingProposalListCreateView(generics.ListCreateAPIView):
    permission_classes = [IsAuthenticated]

    def get_serializer_class(self):
        if self.request.method == "POST":
            return FundingProposalCreateSerializer
        return FundingProposalSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.ADMIN:
            return FundingProposal.objects.all().select_related("funder")
        if user.role == Role.FUNDER:
            return FundingProposal.objects.filter(funder=user)
        return FundingProposal.objects.none()

    def get_permissions(self):
        if self.request.method == "POST":
            return [IsFunder()]
        return [IsAuthenticated()]

    def perform_create(self, serializer):
        serializer.save(funder=self.request.user)


class FundingProposalDetailView(generics.RetrieveAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = FundingProposalSerializer

    def get_queryset(self):
        user = self.request.user
        if user.role == Role.ADMIN:
            return FundingProposal.objects.all()
        if user.role == Role.FUNDER:
            return FundingProposal.objects.filter(funder=user)
        return FundingProposal.objects.none()


class FundingBudgetView(generics.RetrieveAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = FundingBudgetSerializer

    def get_object(self):
        user = self.request.user
        if user.role == Role.ADMIN:
            proposal = FundingProposal.objects.get(pk=self.kwargs["pk"])
        elif user.role == Role.FUNDER:
            proposal = FundingProposal.objects.get(pk=self.kwargs["pk"], funder=user)
        else:
            from rest_framework.exceptions import PermissionDenied
            raise PermissionDenied()
        return proposal.budget


class AdminApproveFundingView(APIView):
    permission_classes = [IsAdminRole]

    @transaction.atomic
    def post(self, request, pk):
        try:
            proposal = FundingProposal.objects.select_for_update().get(pk=pk)
        except FundingProposal.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        if proposal.status not in (FundingProposalStatus.PROPOSED, FundingProposalStatus.PLEDGED):
            return Response(
                {"detail": f"Cannot approve a proposal with status '{proposal.status}'."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        proposal.status = FundingProposalStatus.APPROVED
        proposal.approved_by = request.user
        proposal.approved_at = timezone.now()
        proposal.save(update_fields=["status", "approved_by", "approved_at"])

        FundingBudget.objects.get_or_create(
            proposal=proposal,
            defaults={
                "total_amount": proposal.total_amount,
                "currency": proposal.currency,
            },
        )

        _log_audit(request.user, "funding_approved", "FundingProposal", str(proposal.pk))
        return Response(FundingProposalSerializer(proposal).data)


class AdminRejectFundingView(APIView):
    permission_classes = [IsAdminRole]

    def post(self, request, pk):
        try:
            proposal = FundingProposal.objects.get(pk=pk)
        except FundingProposal.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        reason = request.data.get("reason", "")
        if not reason:
            return Response({"detail": "reason is required."}, status=status.HTTP_400_BAD_REQUEST)

        if proposal.status in (FundingProposalStatus.APPROVED, FundingProposalStatus.ACTIVE, FundingProposalStatus.COMPLETED):
            return Response({"detail": "Cannot reject an already-approved proposal."}, status=status.HTTP_400_BAD_REQUEST)

        proposal.status = FundingProposalStatus.CANCELLED
        proposal.rejection_reason = reason
        proposal.save(update_fields=["status", "rejection_reason"])

        _log_audit(request.user, "funding_rejected", "FundingProposal", str(proposal.pk), {"reason": reason})
        return Response(FundingProposalSerializer(proposal).data)


class AdminAllocateFundingView(APIView):
    """Reserve budget for a specific job listing. Uses SELECT FOR UPDATE to prevent overspend."""
    permission_classes = [IsAdminRole]

    @transaction.atomic
    def post(self, request, pk):
        from funding.models import FundingAllocation, FundingAllocationStatus
        from jobs.models import JobListing

        try:
            budget = FundingBudget.objects.select_for_update().get(proposal__pk=pk)
        except FundingBudget.DoesNotExist:
            return Response({"detail": "Budget not found."}, status=status.HTTP_404_NOT_FOUND)

        if budget.proposal.status != FundingProposalStatus.APPROVED:
            return Response({"detail": "Can only allocate from approved proposals."}, status=status.HTTP_400_BAD_REQUEST)

        job_listing_id = request.data.get("job_listing_id")
        amount = request.data.get("amount")

        if not job_listing_id or amount is None:
            return Response({"detail": "job_listing_id and amount are required."}, status=status.HTTP_400_BAD_REQUEST)

        try:
            amount = int(amount)
        except (TypeError, ValueError):
            return Response({"detail": "amount must be an integer."}, status=status.HTTP_400_BAD_REQUEST)

        if amount <= 0:
            return Response({"detail": "amount must be positive."}, status=status.HTTP_400_BAD_REQUEST)

        if amount > budget.available_amount:
            return Response(
                {"detail": f"Insufficient budget. Available: {budget.available_amount}, requested: {amount}."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        try:
            job = JobListing.objects.get(pk=job_listing_id)
        except JobListing.DoesNotExist:
            return Response({"detail": "Job listing not found."}, status=status.HTTP_404_NOT_FOUND)

        if FundingAllocation.objects.filter(job_listing=job).exists():
            return Response({"detail": "This job already has a funding allocation."}, status=status.HTTP_409_CONFLICT)

        allocation = FundingAllocation.objects.create(
            budget=budget,
            job_listing=job,
            amount=amount,
            currency=budget.currency,
            status=FundingAllocationStatus.RESERVED,
        )

        budget.reserved_amount = budget.reserved_amount + amount
        budget.save(update_fields=["reserved_amount"])

        job.is_funded = True
        job.save(update_fields=["is_funded"])

        _log_audit(request.user, "funding_allocated", "FundingAllocation", str(allocation.pk), {"amount": amount})
        from funding.serializers import FundingAllocationSerializer
        return Response(FundingAllocationSerializer(allocation).data, status=status.HTTP_201_CREATED)


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
