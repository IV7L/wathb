"""
End-to-end integration tests for the four canonical scenarios:
  A – Happy path (worker applies, gets offered, accepts, completes, business pays, worker acks)
  B – Disputed wage (worker disputes payment declaration)
  C – Funded job (job backed by a funding proposal, allocation required before publish)
  D – Insufficient funding (funded job blocked without allocation)
"""
import datetime

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile, WorkerProfile
from assignments.models import Assignment, AssignmentStatus
from disputes.models import Dispute, DisputeStatus
from funding.models import FundingProposal, FundingProposalStatus, FundingBudget, FundingAllocation
from jobs.models import JobListing, JobListingStatus, JobApplication, ApplicationStatus
from wages.models import WageObligation, WageObligationStatus, PaymentDeclaration, PaymentDeclarationStatus


PASSWORD = "SecurePass1!"


def make_user(email, role, **kwargs):
    return User.objects.create_user(email=email, password=PASSWORD, role=role, **kwargs)


def make_business(email):
    user = make_user(email, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user, legal_name=f"Co {email}", contact_name="Owner",
        contact_phone="0599000000", verification_status="APPROVED",
    )
    user.is_verified = True
    user.save(update_fields=["is_verified"])
    return user, bp


def make_worker(email):
    user = make_user(email, Role.WORKER)
    WorkerProfile.objects.create(user=user, full_name=f"Worker {email}")
    return user


def make_admin(email):
    return make_user(email, Role.ADMIN, is_staff=True)


def make_funder(email):
    return make_user(email, Role.FUNDER)


def make_job(biz_profile, is_funded=False, job_status=JobListingStatus.PENDING_REVIEW,
             wage_amount=10000):
    return JobListing.objects.create(
        business=biz_profile,
        title="Test Job",
        description="Daily work",
        location_address="Main St",
        location_city="Gaza City",
        work_date=datetime.date.today() + datetime.timedelta(days=1),
        start_time="08:00",
        end_time="17:00",
        num_workers_needed=1,
        wage_amount=wage_amount,
        wage_currency="ILS",
        wage_basis="PER_DAY",
        payment_method="CASH",
        payment_deadline_days=3,
        status=job_status,
        is_funded=is_funded,
    )


class ScenarioAHappyPath(APITestCase):
    """
    Full lifecycle: apply → offer → accept → completion → confirm → declare payment → acknowledge
    """

    def setUp(self):
        self.biz_user, self.biz_profile = make_business("biz@scenA.com")
        self.worker = make_worker("worker@scenA.com")
        self.job = make_job(self.biz_profile, job_status=JobListingStatus.PUBLISHED)

    def test_full_happy_path(self):
        # Step 1: Worker applies
        self.client.force_authenticate(user=self.worker)
        r = self.client.post(reverse("job-apply", args=[self.job.pk]))
        self.assertEqual(r.status_code, status.HTTP_201_CREATED)
        app_id = r.data["id"]

        # Step 2: Business offers
        self.client.force_authenticate(user=self.biz_user)
        r = self.client.patch(
            reverse("application-action", args=[self.job.pk, app_id]),
            {"action": "offer"},
        )
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        assignment = Assignment.objects.get(job=self.job, worker=self.worker)
        self.assertEqual(assignment.status, AssignmentStatus.OFFERED)

        # Step 3: Worker accepts
        self.client.force_authenticate(user=self.worker)
        r = self.client.post(reverse("assignment-accept", args=[assignment.pk]))
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        assignment.refresh_from_db()
        self.assertEqual(assignment.status, AssignmentStatus.ACCEPTED)
        self.job.refresh_from_db()
        self.assertEqual(self.job.status, JobListingStatus.FILLED)

        # Step 4: Snapshot captured and contains correct wage
        self.assertEqual(assignment.terms_snapshot["wage_amount"], 10000)

        # Step 5: Worker submits completion
        r = self.client.post(
            reverse("completion", args=[assignment.pk]),
            {"notes": "Work done"},
        )
        self.assertEqual(r.status_code, status.HTTP_201_CREATED, r.data)

        # Step 6: Business confirms completion → creates wage obligation
        self.client.force_authenticate(user=self.biz_user)
        r = self.client.patch(reverse("completion-confirm", args=[assignment.pk]))
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        assignment.refresh_from_db()
        self.assertEqual(assignment.status, AssignmentStatus.COMPLETED)
        obligation = WageObligation.objects.get(assignment=assignment)
        self.assertEqual(obligation.status, WageObligationStatus.PENDING)
        self.assertEqual(obligation.amount, 10000)

        # Step 7: Business declares payment
        r = self.client.post(
            reverse("wage-declare-payment", args=[obligation.pk]),
            {
                "declared_amount": 10000,
                "currency": "ILS",
                "payment_date": str(datetime.date.today()),
                "payment_method": "CASH",
            },
        )
        self.assertEqual(r.status_code, status.HTTP_201_CREATED, r.data)
        obligation.refresh_from_db()
        self.assertEqual(obligation.status, WageObligationStatus.AWAITING_ACK)

        # Step 8: Worker acknowledges cash receipt — final verification for Gaza cash economy
        self.client.force_authenticate(user=self.worker)
        r = self.client.post(reverse("wage-acknowledge", args=[obligation.pk]))
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        obligation.refresh_from_db()
        self.assertEqual(obligation.status, WageObligationStatus.VERIFIED_PAID)


class ScenarioBDisputedWage(APITestCase):
    """
    Worker disputes a payment declaration → admin resolves.
    """

    def setUp(self):
        self.admin = make_admin("admin@scenB.com")
        self.biz_user, self.biz_profile = make_business("biz@scenB.com")
        self.worker = make_worker("worker@scenB.com")
        self.job = make_job(self.biz_profile, job_status=JobListingStatus.PUBLISHED)

    def test_disputed_wage_scenario(self):
        # Setup: offer → accept → completion confirmed → declare
        app = JobApplication.objects.create(job=self.job, worker=self.worker)
        assignment = Assignment.objects.create(
            job=self.job, worker=self.worker, application=app,
            terms_snapshot=self.job.material_terms_snapshot(),
            status=AssignmentStatus.ACCEPTED,
        )
        from assignments.models import CompletionRecord, CompletionStatus
        CompletionRecord.objects.create(
            assignment=assignment, submitted_by=self.worker,
            status=CompletionStatus.CONFIRMED, confirmed_by=self.biz_user,
        )
        assignment.status = AssignmentStatus.COMPLETED
        assignment.save(update_fields=["status"])

        obligation = WageObligation.objects.create(
            assignment=assignment, amount=10000, currency="ILS",
            due_date=datetime.date.today() + datetime.timedelta(days=3),
            status=WageObligationStatus.AWAITING_ACK,
        )
        PaymentDeclaration.objects.create(
            wage_obligation=obligation,
            declared_by=self.biz_user,
            declared_amount=10000,
            currency="ILS",
            payment_date=datetime.date.today(),
            payment_method="CASH",
            status=PaymentDeclarationStatus.SUBMITTED,
        )

        # Worker disputes
        self.client.force_authenticate(user=self.worker)
        r = self.client.post(
            reverse("wage-dispute-payment", args=[obligation.pk]),
            {"category": "PAYMENT_NOT_RECEIVED", "description": "I was not paid."},
        )
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        obligation.refresh_from_db()
        self.assertEqual(obligation.status, WageObligationStatus.DISPUTED)

        dispute = Dispute.objects.get(assignment=assignment)
        self.assertEqual(dispute.status, DisputeStatus.OPEN)

        # Business sees the dispute
        self.client.force_authenticate(user=self.biz_user)
        r = self.client.get(reverse("dispute-list"))
        self.assertEqual(r.status_code, status.HTTP_200_OK)
        ids = [str(d["id"]) for d in r.data["results"]]
        self.assertIn(str(dispute.pk), ids)

        # Admin assigns and resolves
        self.client.force_authenticate(user=self.admin)
        r = self.client.post(reverse("admin-dispute-assign", args=[dispute.pk]))
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)

        r = self.client.post(
            reverse("admin-dispute-resolve", args=[dispute.pk]),
            {"resolution_notes": "Business confirmed non-payment. Worker compensated."},
        )
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        dispute.refresh_from_db()
        self.assertEqual(dispute.status, DisputeStatus.RESOLVED)


class ScenarioCFundedJob(APITestCase):
    """
    Funded job: proposal approved → budget created → allocation reserved → job published.
    """

    def setUp(self):
        self.admin = make_admin("admin@scenC.com")
        self.funder = make_funder("funder@scenC.com")
        self.biz_user, self.biz_profile = make_business("biz@scenC.com")
        self.worker = make_worker("worker@scenC.com")

    def test_funded_job_lifecycle(self):
        # Step 1: Funder creates proposal
        self.client.force_authenticate(user=self.funder)
        r = self.client.post(
            reverse("funding-proposal-list"),
            {"title": "Gaza Workers Fund", "description": "...",
             "total_amount": 200000, "currency": "ILS"},
        )
        self.assertEqual(r.status_code, status.HTTP_201_CREATED, r.data)
        proposal = FundingProposal.objects.get(funder=self.funder, title="Gaza Workers Fund")
        proposal_id = proposal.pk

        # Step 2: Admin approves
        self.client.force_authenticate(user=self.admin)
        r = self.client.post(reverse("admin-funding-approve", args=[proposal_id]))
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        budget = FundingBudget.objects.get(proposal__pk=proposal_id)
        self.assertEqual(budget.total_amount, 200000)
        self.assertEqual(budget.available_amount, 200000)

        # Step 3: Create funded job (needs allocation before publish)
        job = make_job(self.biz_profile, is_funded=True,
                       job_status=JobListingStatus.PENDING_REVIEW, wage_amount=10000)

        # Step 4: Without allocation, cannot publish
        self.client.force_authenticate(user=self.biz_user)
        r = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(r.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("allocation", r.data["detail"].lower())

        # Step 5: Create allocation directly (admin/funder sets this up)
        FundingAllocation.objects.create(
            budget=budget, job_listing=job, amount=10000, currency="ILS",
            status="RESERVED",
        )

        # Step 6: Now can publish
        r = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(r.status_code, status.HTTP_200_OK, r.data)
        job.refresh_from_db()
        self.assertEqual(job.status, JobListingStatus.PUBLISHED)


class ScenarioDInsufficientFunding(APITestCase):
    """
    Funded job where the allocation does not exist → publish blocked.
    """

    def setUp(self):
        self.biz_user, self.biz_profile = make_business("biz@scenD.com")

    def test_funded_job_blocked_without_allocation(self):
        job = make_job(self.biz_profile, is_funded=True,
                       job_status=JobListingStatus.PENDING_REVIEW)
        self.client.force_authenticate(user=self.biz_user)
        r = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(r.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("allocation", r.data["detail"].lower())
        job.refresh_from_db()
        self.assertEqual(job.status, JobListingStatus.PENDING_REVIEW)

    def test_funded_job_with_released_allocation_still_blocked(self):
        funder = make_funder("funder@scenD.com")
        proposal = FundingProposal.objects.create(
            funder=funder, title="Fund", description="...",
            total_amount=100000, currency="ILS",
            status=FundingProposalStatus.APPROVED,
        )
        budget = FundingBudget.objects.create(
            proposal=proposal, total_amount=100000, currency="ILS"
        )
        job = make_job(self.biz_profile, is_funded=True,
                       job_status=JobListingStatus.PENDING_REVIEW)
        FundingAllocation.objects.create(
            budget=budget, job_listing=job, amount=10000, currency="ILS",
            status="RELEASED",
        )
        self.client.force_authenticate(user=self.biz_user)
        r = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(r.status_code, status.HTTP_400_BAD_REQUEST)
