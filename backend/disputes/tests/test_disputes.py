import datetime

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile, WorkerProfile
from assignments.models import Assignment, AssignmentStatus
from disputes.models import Dispute, DisputeStatus, DisputeEvent
from jobs.models import JobListing, JobListingStatus, JobApplication, ApplicationStatus
from wages.models import WageObligation, WageObligationStatus


def make_user(phone, role, password="P@ss1234", **kwargs):
    return User.objects.create_user(phone, password, role=role, **kwargs)


def make_business(phone="+970599011001"):
    user = make_user(phone, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user, legal_name="Dispute Co", contact_name="Owner",
        contact_phone="0599000200", verification_status="APPROVED",
    )
    user.is_verified = True
    user.save(update_fields=["is_verified"])
    return user, bp


def make_worker(phone="+970599011002"):
    user = make_user(phone, Role.WORKER)
    WorkerProfile.objects.create(user=user, full_name="Dispute Worker")
    return user


def make_job_and_assignment(biz_profile, worker_user):
    job = JobListing.objects.create(
        business=biz_profile,
        title="Laborer",
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
        status=JobListingStatus.PUBLISHED,
    )
    app = JobApplication.objects.create(
        job=job, worker=worker_user, status=ApplicationStatus.ACCEPTED
    )
    assignment = Assignment.objects.create(
        job=job, worker=worker_user, application=app,
        terms_snapshot=job.material_terms_snapshot(),
        status=AssignmentStatus.COMPLETED,
    )
    return job, assignment


def make_dispute(assignment, worker_user, obligation=None):
    return Dispute.objects.create(
        assignment=assignment,
        wage_obligation=obligation,
        raised_by=worker_user,
        category="PAYMENT_NOT_RECEIVED",
        description="Not paid",
    )


class DisputeVisibilityTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.dispute = make_dispute(self.assignment, self.worker)

    def test_worker_can_see_own_dispute(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.get(reverse("dispute-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [str(d["id"]) for d in response.data["results"]]
        self.assertIn(str(self.dispute.pk), ids)

    def test_business_can_see_dispute_on_their_assignment(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.get(reverse("dispute-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [str(d["id"]) for d in response.data["results"]]
        self.assertIn(str(self.dispute.pk), ids)

    def test_other_business_cannot_see_dispute(self):
        other_biz_user, _ = make_business("+970599011003")
        self.client.force_authenticate(user=other_biz_user)
        response = self.client.get(reverse("dispute-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [str(d["id"]) for d in response.data["results"]]
        self.assertNotIn(str(self.dispute.pk), ids)

    def test_admin_can_see_all_disputes(self):
        admin = make_user("+970599011004", Role.ADMIN, is_staff=True)
        other_biz_user, other_biz_profile = make_business("+970599011005")
        other_worker = make_worker("+970599011006")
        other_job, other_assignment = make_job_and_assignment(other_biz_profile, other_worker)
        other_dispute = make_dispute(other_assignment, other_worker)

        self.client.force_authenticate(user=admin)
        response = self.client.get(reverse("dispute-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        ids = [str(d["id"]) for d in response.data["results"]]
        self.assertIn(str(self.dispute.pk), ids)
        self.assertIn(str(other_dispute.pk), ids)

    def test_business_can_retrieve_dispute_detail(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.get(reverse("dispute-detail", args=[self.dispute.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_other_worker_cannot_see_dispute_detail(self):
        other_worker = make_worker("+970599011010")
        self.client.force_authenticate(user=other_worker)
        response = self.client.get(reverse("dispute-detail", args=[self.dispute.pk]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class AdminDisputeWorkflowTests(APITestCase):

    def setUp(self):
        self.admin = make_user("+970599011007", Role.ADMIN, is_staff=True)
        self.biz_user, self.biz_profile = make_business("+970599011008")
        self.worker = make_worker("+970599011009")
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.dispute = make_dispute(self.assignment, self.worker)

    def test_admin_can_assign_dispute(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-dispute-assign", args=[self.dispute.pk])
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.dispute.refresh_from_db()
        self.assertEqual(self.dispute.status, DisputeStatus.IN_REVIEW)
        self.assertEqual(self.dispute.assigned_to, self.admin)

    def test_admin_can_resolve_dispute(self):
        self.dispute.status = DisputeStatus.IN_REVIEW
        self.dispute.save(update_fields=["status"])
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-dispute-resolve", args=[self.dispute.pk]),
            {"resolution_notes": "Payment confirmed by bank records."},
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.dispute.refresh_from_db()
        self.assertEqual(self.dispute.status, DisputeStatus.RESOLVED)

    def test_resolve_requires_resolution_notes(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-dispute-resolve", args=[self.dispute.pk]),
            {},
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_assign_already_in_review_dispute(self):
        self.dispute.status = DisputeStatus.IN_REVIEW
        self.dispute.save(update_fields=["status"])
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(
            reverse("admin-dispute-assign", args=[self.dispute.pk])
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_worker_cannot_resolve_dispute(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("admin-dispute-resolve", args=[self.dispute.pk]),
            {"resolution_notes": "Tried to resolve."},
        )
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_resolve_creates_audit_event(self):
        from audit.models import AuditEvent
        self.dispute.status = DisputeStatus.IN_REVIEW
        self.dispute.save(update_fields=["status"])
        self.client.force_authenticate(user=self.admin)
        self.client.post(
            reverse("admin-dispute-resolve", args=[self.dispute.pk]),
            {"resolution_notes": "Resolved by admin."},
        )
        self.assertTrue(
            AuditEvent.objects.filter(
                action="dispute_resolved",
                object_id=str(self.dispute.pk),
            ).exists()
        )


class DisputeEventTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business("+970599011011")
        self.worker = make_worker("+970599011012")
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.dispute = make_dispute(self.assignment, self.worker)
        DisputeEvent.objects.create(
            dispute=self.dispute, actor=self.worker, action="opened", notes="Filed"
        )

    def test_worker_can_see_own_dispute_events(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.get(reverse("dispute-events", args=[self.dispute.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["count"], 1)

    def test_business_can_see_events_on_their_dispute(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.get(reverse("dispute-events", args=[self.dispute.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["count"], 1)
