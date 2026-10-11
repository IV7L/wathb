import datetime

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile, WorkerProfile
from assignments.models import (
    Assignment, AssignmentStatus, AttendanceRecord, AttendanceStatus,
    CompletionRecord, CompletionStatus,
)
from jobs.models import JobListing, JobListingStatus, JobApplication, ApplicationStatus
from wages.models import WageObligation


def make_user(email, role, password="P@ss1234", **kwargs):
    return User.objects.create_user(email=email, password=password, role=role, **kwargs)


def make_business(email="biz@test.com"):
    user = make_user(email, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user, legal_name="Test Co", contact_name="Owner",
        contact_phone="0599000000", verification_status="APPROVED",
    )
    user.is_verified = True
    user.save(update_fields=["is_verified"])
    return user, bp


def make_worker(email="worker@test.com"):
    user = make_user(email, Role.WORKER)
    WorkerProfile.objects.create(user=user, full_name="Test Worker")
    return user


def make_job(business_profile, **kwargs):
    defaults = dict(
        title="Day Laborer",
        description="Help needed",
        location_address="123 St",
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
        is_funded=False,
    )
    defaults.update(kwargs)
    return JobListing.objects.create(business=business_profile, **defaults)


def make_assignment(job, worker, app_status=ApplicationStatus.OFFERED,
                    assign_status=AssignmentStatus.OFFERED):
    app = JobApplication.objects.create(job=job, worker=worker, status=app_status)
    return Assignment.objects.create(
        job=job,
        worker=worker,
        application=app,
        terms_snapshot={},
        status=assign_status,
    )


class AssignmentAcceptTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile)
        self.assignment = make_assignment(self.job, self.worker)

    def test_worker_can_accept_offered_assignment(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assignment.refresh_from_db()
        self.assertEqual(self.assignment.status, AssignmentStatus.ACCEPTED)

    def test_accept_captures_terms_snapshot(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        self.assignment.refresh_from_db()
        snapshot = self.assignment.terms_snapshot
        self.assertEqual(snapshot["wage_amount"], self.job.wage_amount)
        self.assertEqual(snapshot["wage_currency"], self.job.wage_currency)
        self.assertEqual(snapshot["payment_deadline_days"], self.job.payment_deadline_days)
        self.assertEqual(str(self.job.id), snapshot["job_id"])

    def test_snapshot_preserves_original_wage_after_job_edit(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        self.assignment.refresh_from_db()
        original_wage = self.assignment.terms_snapshot["wage_amount"]

        self.job.wage_amount = 99999
        self.job.save(update_fields=["wage_amount"])

        self.assignment.refresh_from_db()
        self.assertEqual(self.assignment.terms_snapshot["wage_amount"], original_wage)
        self.assertNotEqual(self.assignment.terms_snapshot["wage_amount"], 99999)

    def test_accept_transitions_job_to_filled(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        self.job.refresh_from_db()
        self.assertEqual(self.job.status, JobListingStatus.FILLED)

    def test_cannot_accept_already_accepted_assignment(self):
        self.assignment.status = AssignmentStatus.ACCEPTED
        self.assignment.save(update_fields=["status"])
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_worker_cannot_accept_another_workers_assignment(self):
        other_worker = make_worker("other@test.com")
        self.client.force_authenticate(user=other_worker)
        response = self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_assignment_uniqueness_per_job_worker(self):
        from django.db import IntegrityError
        with self.assertRaises(IntegrityError):
            app2 = JobApplication.objects.create(
                job=self.job, worker=make_worker("w2@test.com"), status=ApplicationStatus.OFFERED
            )
            Assignment.objects.create(
                job=self.job,
                worker=self.worker,
                application=app2,
                terms_snapshot={},
                status=AssignmentStatus.OFFERED,
            )


class AssignmentDeclineTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile)
        self.assignment = make_assignment(self.job, self.worker)

    def test_worker_can_decline_offered_assignment(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("assignment-decline", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assignment.refresh_from_db()
        self.assertEqual(self.assignment.status, AssignmentStatus.DECLINED)

    def test_cannot_decline_accepted_assignment(self):
        self.assignment.status = AssignmentStatus.ACCEPTED
        self.assignment.save(update_fields=["status"])
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("assignment-decline", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class AttendanceTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile)
        self.assignment = make_assignment(
            self.job, self.worker, assign_status=AssignmentStatus.ACCEPTED
        )
        self.assignment.terms_snapshot = self.job.material_terms_snapshot()
        self.assignment.save(update_fields=["terms_snapshot"])

    def test_worker_can_submit_attendance(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("attendance-list", args=[self.assignment.pk]),
            {"work_date": str(self.job.work_date)},
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_business_can_confirm_worker_attendance(self):
        AttendanceRecord.objects.create(
            assignment=self.assignment,
            submitted_by=self.worker,
            work_date=self.job.work_date,
            status=AttendanceStatus.SUBMITTED,
        )
        record = AttendanceRecord.objects.get(assignment=self.assignment)
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.patch(
            reverse("attendance-confirm", args=[self.assignment.pk, record.pk])
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        record.refresh_from_db()
        self.assertEqual(record.status, AttendanceStatus.CONFIRMED)

    def test_worker_cannot_confirm_own_attendance(self):
        AttendanceRecord.objects.create(
            assignment=self.assignment,
            submitted_by=self.worker,
            work_date=self.job.work_date,
            status=AttendanceStatus.SUBMITTED,
        )
        record = AttendanceRecord.objects.get(assignment=self.assignment)
        self.client.force_authenticate(user=self.worker)
        response = self.client.patch(
            reverse("attendance-confirm", args=[self.assignment.pk, record.pk])
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_attendance_on_inactive_assignment_rejected(self):
        self.assignment.status = AssignmentStatus.OFFERED
        self.assignment.save(update_fields=["status"])
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("attendance-list", args=[self.assignment.pk]),
            {"work_date": str(self.job.work_date)},
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class CompletionTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile)
        self.assignment = make_assignment(
            self.job, self.worker, assign_status=AssignmentStatus.ACCEPTED
        )
        self.assignment.terms_snapshot = self.job.material_terms_snapshot()
        self.assignment.save(update_fields=["terms_snapshot"])

    def test_worker_can_submit_completion(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("completion", args=[self.assignment.pk]),
            {"notes": "Done"},
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_duplicate_completion_submission_returns_409(self):
        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.worker,
            status=CompletionStatus.SUBMITTED,
        )
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("completion", args=[self.assignment.pk]),
            {"notes": "Again"},
        )
        self.assertEqual(response.status_code, status.HTTP_409_CONFLICT)

    def test_business_can_confirm_worker_completion(self):
        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.worker,
            status=CompletionStatus.SUBMITTED,
        )
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.patch(reverse("completion-confirm", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assignment.refresh_from_db()
        self.assertEqual(self.assignment.status, AssignmentStatus.COMPLETED)

    def test_worker_cannot_confirm_own_completion(self):
        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.worker,
            status=CompletionStatus.SUBMITTED,
        )
        self.client.force_authenticate(user=self.worker)
        response = self.client.patch(reverse("completion-confirm", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("Cannot confirm your own", response.data["detail"])

    def test_business_cannot_confirm_own_completion_submission(self):
        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.biz_user,
            status=CompletionStatus.SUBMITTED,
        )
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.patch(reverse("completion-confirm", args=[self.assignment.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_confirmation_creates_wage_obligation(self):
        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.worker,
            status=CompletionStatus.SUBMITTED,
        )
        self.client.force_authenticate(user=self.biz_user)
        self.client.patch(reverse("completion-confirm", args=[self.assignment.pk]))
        self.assertTrue(WageObligation.objects.filter(assignment=self.assignment).exists())

    def test_wage_obligation_amount_from_snapshot_not_current_job(self):
        original_wage = self.job.wage_amount
        self.assignment.terms_snapshot = self.job.material_terms_snapshot()
        self.assignment.save(update_fields=["terms_snapshot"])

        self.job.wage_amount = 99999
        self.job.save(update_fields=["wage_amount"])

        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.worker,
            status=CompletionStatus.SUBMITTED,
        )
        self.client.force_authenticate(user=self.biz_user)
        self.client.patch(reverse("completion-confirm", args=[self.assignment.pk]))

        obligation = WageObligation.objects.get(assignment=self.assignment)
        self.assertEqual(obligation.amount, original_wage)
        self.assertNotEqual(obligation.amount, 99999)

    def test_wage_obligation_uniqueness(self):
        CompletionRecord.objects.create(
            assignment=self.assignment, submitted_by=self.worker,
            status=CompletionStatus.SUBMITTED,
        )
        from django.db import IntegrityError
        from wages.models import WageObligation
        WageObligation.objects.create(
            assignment=self.assignment, amount=10000, currency="ILS",
            due_date=datetime.date.today() + datetime.timedelta(days=3),
        )
        with self.assertRaises(IntegrityError):
            WageObligation.objects.create(
                assignment=self.assignment, amount=10000, currency="ILS",
                due_date=datetime.date.today() + datetime.timedelta(days=3),
            )


class AssignmentSerializerTermsTests(APITestCase):
    """Job terms are embedded in AssignmentSerializer for pre-acceptance review."""

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile)
        self.assignment = make_assignment(self.job, self.worker)

    def test_offered_assignment_list_contains_job_terms(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.get(reverse("assignment-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        item = response.data["results"][0]
        self.assertEqual(str(item["job_work_date"]), str(self.job.work_date))
        self.assertEqual(item["job_wage_amount"], self.job.wage_amount)
        self.assertEqual(item["job_wage_currency"], self.job.wage_currency)
        self.assertEqual(item["job_payment_method"], self.job.payment_method)
        self.assertEqual(item["job_payment_deadline_days"], self.job.payment_deadline_days)
        self.assertEqual(item["job_location_city"], self.job.location_city)

    def test_job_terms_survive_after_accept(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(reverse("assignment-accept", args=[self.assignment.pk]))
        response = self.client.get(reverse("assignment-list"))
        item = response.data["results"][0]
        # Live fields still reflect the job (they come from the FK, not the snapshot)
        self.assertEqual(item["job_wage_amount"], self.job.wage_amount)
        self.assertEqual(item["status"], AssignmentStatus.ACCEPTED)
