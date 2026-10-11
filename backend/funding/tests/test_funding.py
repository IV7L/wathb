import datetime

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile
from funding.models import (
    FundingProposal, FundingProposalStatus, FundingBudget, FundingAllocation
)
from jobs.models import JobListing, JobListingStatus


def make_user(email, role, password="P@ss1234", **kwargs):
    return User.objects.create_user(email=email, password=password, role=role, **kwargs)


def make_business(email="biz@funding.com"):
    user = make_user(email, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user, legal_name="Funding Co", contact_name="Owner",
        contact_phone="0599000300", verification_status="APPROVED",
    )
    user.is_verified = True
    user.save(update_fields=["is_verified"])
    return user, bp


def make_proposal(funder, total_amount=100000, proposal_status=FundingProposalStatus.PROPOSED):
    return FundingProposal.objects.create(
        funder=funder,
        title="Gaza Relief Fund",
        description="Supporting daily workers",
        total_amount=total_amount,
        currency="ILS",
        status=proposal_status,
    )


def make_job(biz_profile, is_funded=False, job_status=JobListingStatus.PENDING_REVIEW):
    return JobListing.objects.create(
        business=biz_profile,
        title="Funded Job",
        description="Work",
        location_address="Gaza",
        location_city="Gaza City",
        work_date=datetime.date.today() + datetime.timedelta(days=1),
        start_time="08:00",
        end_time="17:00",
        num_workers_needed=1,
        wage_amount=10000,
        wage_currency="ILS",
        wage_basis="PER_DAY",
        payment_method="CASH",
        payment_deadline_days=3,
        status=job_status,
        is_funded=is_funded,
    )


class FundingBudgetAccountingTests(APITestCase):

    def test_available_amount_formula(self):
        funder = make_user("funder@budget.com", Role.FUNDER)
        proposal = make_proposal(funder, total_amount=100000, proposal_status=FundingProposalStatus.APPROVED)
        budget = FundingBudget.objects.create(
            proposal=proposal, total_amount=100000, currency="ILS",
            reserved_amount=10000, allocated_amount=20000, spent_amount=5000,
        )
        self.assertEqual(budget.available_amount, 65000)

    def test_available_amount_zero_at_full_allocation(self):
        funder = make_user("funder@budget2.com", Role.FUNDER)
        proposal = make_proposal(funder, total_amount=50000, proposal_status=FundingProposalStatus.APPROVED)
        budget = FundingBudget.objects.create(
            proposal=proposal, total_amount=50000, currency="ILS",
            reserved_amount=25000, allocated_amount=25000, spent_amount=0,
        )
        self.assertEqual(budget.available_amount, 0)


class AdminApproveFundingTests(APITestCase):

    def setUp(self):
        self.admin = make_user("admin@funding.com", Role.ADMIN, is_staff=True)
        self.funder = make_user("funder@ftest.com", Role.FUNDER)
        self.proposal = make_proposal(self.funder)

    def test_admin_can_approve_proposed_funding(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-funding-approve", args=[self.proposal.pk])
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.proposal.refresh_from_db()
        self.assertEqual(self.proposal.status, FundingProposalStatus.APPROVED)

    def test_approval_creates_budget(self):
        self.client.force_authenticate(user=self.admin)
        self.client.post(reverse("admin-funding-approve", args=[self.proposal.pk]))
        self.assertTrue(FundingBudget.objects.filter(proposal=self.proposal).exists())
        budget = FundingBudget.objects.get(proposal=self.proposal)
        self.assertEqual(budget.total_amount, self.proposal.total_amount)
        self.assertEqual(budget.currency, self.proposal.currency)

    def test_approving_already_approved_proposal_is_blocked(self):
        """Re-approving returns 400; only one budget is ever created."""
        self.client.force_authenticate(user=self.admin)
        self.client.post(reverse("admin-funding-approve", args=[self.proposal.pk]))
        response = self.client.post(reverse("admin-funding-approve", args=[self.proposal.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(FundingBudget.objects.filter(proposal=self.proposal).count(), 1)

    def test_admin_can_reject_proposed_funding(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-funding-reject", args=[self.proposal.pk]),
            {"reason": "Insufficient documentation"},
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.proposal.refresh_from_db()
        self.assertEqual(self.proposal.status, FundingProposalStatus.CANCELLED)

    def test_reject_requires_reason(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-funding-reject", args=[self.proposal.pk]),
            {},
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_reject_approved_proposal(self):
        self.proposal.status = FundingProposalStatus.APPROVED
        self.proposal.save(update_fields=["status"])
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-funding-reject", args=[self.proposal.pk]),
            {"reason": "Changed mind"},
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_worker_cannot_approve_funding(self):
        worker = make_user("w@funding.com", Role.WORKER)
        self.client.force_authenticate(user=worker)
        response = self.client.post(
            reverse("admin-funding-approve", args=[self.proposal.pk])
        )
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_approval_logs_audit_event(self):
        from audit.models import AuditEvent
        self.client.force_authenticate(user=self.admin)
        self.client.post(reverse("admin-funding-approve", args=[self.proposal.pk]))
        self.assertTrue(
            AuditEvent.objects.filter(
                action="funding_approved",
                object_id=str(self.proposal.pk),
            ).exists()
        )


class FunderProposalAccessTests(APITestCase):

    def setUp(self):
        self.funder = make_user("funder@access.com", Role.FUNDER)
        self.other_funder = make_user("other_funder@access.com", Role.FUNDER)
        self.proposal = make_proposal(self.funder)

    def test_funder_can_create_proposal(self):
        self.client.force_authenticate(user=self.funder)
        response = self.client.post(
            reverse("funding-proposal-list"),
            {
                "title": "New Fund",
                "description": "For workers",
                "total_amount": 50000,
                "currency": "ILS",
            },
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_worker_cannot_create_proposal(self):
        worker = make_user("w@prop.com", Role.WORKER)
        self.client.force_authenticate(user=worker)
        response = self.client.post(
            reverse("funding-proposal-list"),
            {"title": "Fund", "description": "...", "total_amount": 50000, "currency": "ILS"},
        )
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_funder_can_only_see_own_proposals(self):
        make_proposal(self.other_funder)
        self.client.force_authenticate(user=self.funder)
        response = self.client.get(reverse("funding-proposal-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [str(p["id"]) for p in response.data["results"]]
        self.assertIn(str(self.proposal.pk), ids)
        for item_id in ids:
            p = FundingProposal.objects.get(pk=item_id)
            self.assertEqual(p.funder, self.funder)


class AdminAllocateFundingTests(APITestCase):

    def setUp(self):
        import uuid as _uuid
        self.uuid = _uuid
        self.admin = make_user("admin@alloc.com", Role.ADMIN, is_staff=True)
        self.funder = make_user("funder@alloc.com", Role.FUNDER)
        self.biz_user, self.biz_profile = make_business("biz@alloc.com")

        self.proposal = make_proposal(
            self.funder, total_amount=500_000, proposal_status=FundingProposalStatus.APPROVED
        )
        self.budget = FundingBudget.objects.create(
            proposal=self.proposal,
            total_amount=500_000,
            currency="ILS",
        )
        self.proposal.approved_by = self.admin
        self.proposal.save(update_fields=["approved_by"])
        self.job = make_job(self.biz_profile)

    def _url(self):
        return reverse("admin-funding-allocate", kwargs={"pk": self.proposal.pk})

    def test_admin_can_allocate_within_budget(self):
        self.client.force_authenticate(user=self.admin)
        data = {"job_listing_id": str(self.job.pk), "amount": 100_000}
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.budget.refresh_from_db()
        self.assertEqual(self.budget.reserved_amount, 100_000)
        self.assertEqual(self.budget.available_amount, 400_000)
        self.job.refresh_from_db()
        self.assertTrue(self.job.is_funded)

    def test_allocation_status_is_reserved(self):
        self.client.force_authenticate(user=self.admin)
        self.client.post(self._url(), {"job_listing_id": str(self.job.pk), "amount": 50_000}, format="json")
        allocation = FundingAllocation.objects.get(job_listing=self.job)
        from funding.models import FundingAllocationStatus
        self.assertEqual(allocation.status, FundingAllocationStatus.RESERVED)

    def test_cannot_over_allocate(self):
        """Budget invariant: reserved + allocated + spent <= total."""
        self.client.force_authenticate(user=self.admin)
        data = {"job_listing_id": str(self.job.pk), "amount": 600_000}
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("Insufficient", response.data["detail"])
        self.budget.refresh_from_db()
        self.assertEqual(self.budget.reserved_amount, 0)

    def test_cannot_allocate_same_job_twice(self):
        """OneToOne on FundingAllocation.job_listing is enforced at the API level."""
        self.client.force_authenticate(user=self.admin)
        data = {"job_listing_id": str(self.job.pk), "amount": 10_000}
        self.client.post(self._url(), data, format="json")
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_409_CONFLICT)
        self.assertEqual(FundingAllocation.objects.filter(job_listing=self.job).count(), 1)

    def test_cannot_allocate_from_unapproved_proposal(self):
        self.proposal.status = FundingProposalStatus.PROPOSED
        self.proposal.save(update_fields=["status"])
        self.client.force_authenticate(user=self.admin)
        data = {"job_listing_id": str(self.job.pk), "amount": 10_000}
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_non_admin_cannot_allocate(self):
        self.client.force_authenticate(user=self.funder)
        data = {"job_listing_id": str(self.job.pk), "amount": 10_000}
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_allocate_nonexistent_job_returns_404(self):
        self.client.force_authenticate(user=self.admin)
        data = {"job_listing_id": str(self.uuid.uuid4()), "amount": 10_000}
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_allocate_zero_amount_returns_400(self):
        self.client.force_authenticate(user=self.admin)
        data = {"job_listing_id": str(self.job.pk), "amount": 0}
        response = self.client.post(self._url(), data, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_allocation_logs_audit_event(self):
        from audit.models import AuditEvent
        self.client.force_authenticate(user=self.admin)
        self.client.post(self._url(), {"job_listing_id": str(self.job.pk), "amount": 10_000}, format="json")
        self.assertTrue(AuditEvent.objects.filter(action="funding_allocated").exists())
