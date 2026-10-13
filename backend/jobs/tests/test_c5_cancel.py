"""C5 cancel tests — job cancel cascade + assignment cancel by worker."""
import datetime

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile, WorkerProfile
from assignments.models import Assignment, AssignmentStatus
from jobs.models import (
    JobListing, JobListingStatus, JobApplication, ApplicationStatus,
)


def make_user(phone, role, password="P@ss1234"):
    return User.objects.create_user(phone, password, role=role)


def make_business(phone="+970599090001"):
    user = make_user(phone, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user, legal_name="Test Co", contact_name="Owner",
        contact_phone="0599000000", verification_status="APPROVED",
    )
    user.is_verified = True
    user.save(update_fields=["is_verified"])
    return user, bp


def make_worker(phone="+970599090002"):
    user = make_user(phone, Role.WORKER)
    WorkerProfile.objects.create(user=user, full_name="Test Worker")
    return user


def make_job(bp, status=JobListingStatus.PUBLISHED, days_ahead=2):
    return JobListing.objects.create(
        business=bp,
        title="Test Job", description="desc", location_address="x",
        location_city="Gaza", work_date=datetime.date.today() + datetime.timedelta(days=days_ahead),
        start_time="08:00", end_time="17:00", num_workers_needed=1,
        wage_amount=5000, wage_currency="ILS", wage_basis="PER_DAY",
        payment_method="CASH", payment_deadline_days=3, status=status,
    )


class JobCancelCascadeTests(APITestCase):

    def setUp(self):
        self.biz_user, self.bp = make_business()
        self.worker = make_worker()
        self.job = make_job(self.bp)
        self.client.force_authenticate(user=self.biz_user)

    def _cancel(self, reason="نهاية الحاجة"):
        return self.client.post(
            reverse("job-cancel", args=[self.job.pk]),
            {"reason": reason},
        )

    def test_cancel_requires_reason(self):
        resp = self.client.post(reverse("job-cancel", args=[self.job.pk]), {})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("reason", resp.data["detail"].lower())

    def test_cancel_stores_reason(self):
        self._cancel("تغيّرت الخطط")
        self.job.refresh_from_db()
        self.assertEqual(self.job.cancellation_reason, "تغيّرت الخطط")

    def test_cancel_cascades_pending_applications(self):
        app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.PENDING
        )
        self._cancel()
        app.refresh_from_db()
        self.assertEqual(app.status, ApplicationStatus.CANCELLED)

    def test_cancel_cascades_offered_applications(self):
        app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.OFFERED
        )
        self._cancel()
        app.refresh_from_db()
        self.assertEqual(app.status, ApplicationStatus.CANCELLED)

    def test_cancel_does_not_touch_accepted_applications(self):
        # ACCEPTED → Assignment already exists; application itself stays ACCEPTED.
        app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.ACCEPTED
        )
        self._cancel()
        app.refresh_from_db()
        self.assertEqual(app.status, ApplicationStatus.ACCEPTED)

    def test_cancel_cascades_accepted_assignments(self):
        app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.ACCEPTED
        )
        assignment = Assignment.objects.create(
            job=self.job, worker=self.worker, application=app,
            terms_snapshot=self.job.material_terms_snapshot(),
            status=AssignmentStatus.ACCEPTED,
        )
        self._cancel()
        assignment.refresh_from_db()
        self.assertEqual(assignment.status, AssignmentStatus.CANCELLED)

    def test_worker_cannot_cancel_job(self):
        self.client.force_authenticate(user=self.worker)
        resp = self._cancel()
        self.assertEqual(resp.status_code, status.HTTP_403_FORBIDDEN)


class AssignmentCancelWorkerTests(APITestCase):

    def setUp(self):
        self.biz_user, self.bp = make_business("+970599090003")
        self.worker = make_worker("+970599090004")
        self.job = make_job(self.bp, days_ahead=3)
        app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.ACCEPTED
        )
        self.assignment = Assignment.objects.create(
            job=self.job, worker=self.worker, application=app,
            terms_snapshot=self.job.material_terms_snapshot(),
            status=AssignmentStatus.ACCEPTED,
        )

    def _cancel_as(self, user, reason="لا أستطيع الحضور"):
        self.client.force_authenticate(user=user)
        return self.client.post(
            reverse("assignment-cancel", args=[self.assignment.pk]),
            {"reason": reason},
        )

    def test_worker_can_cancel_own_assignment(self):
        resp = self._cancel_as(self.worker)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assignment.refresh_from_db()
        self.assertEqual(self.assignment.status, AssignmentStatus.CANCELLED)
        self.assertEqual(self.assignment.cancellation_reason, "لا أستطيع الحضور")

    def test_worker_cannot_cancel_other_assignment(self):
        other_worker = make_worker("+970599090005")
        self.client.force_authenticate(user=other_worker)
        resp = self.client.post(
            reverse("assignment-cancel", args=[self.assignment.pk]),
            {"reason": "x"},
        )
        self.assertEqual(resp.status_code, status.HTTP_404_NOT_FOUND)

    def test_cancel_requires_reason(self):
        resp = self._cancel_as(self.worker, reason="")
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cancel_rejected_after_work_date(self):
        self.job.work_date = datetime.date.today() - datetime.timedelta(days=1)
        self.job.save(update_fields=["work_date"])
        resp = self._cancel_as(self.worker)
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("work date", resp.data["detail"].lower())

    def test_business_can_still_cancel_assignment(self):
        resp = self._cancel_as(self.biz_user)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
