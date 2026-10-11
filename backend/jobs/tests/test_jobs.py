import datetime

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile, WorkerProfile
from assignments.models import Assignment, AssignmentStatus
from funding.models import FundingProposal, FundingBudget, FundingAllocation
from jobs.models import JobListing, JobListingStatus, JobApplication, ApplicationStatus


def make_user(email, role, password="P@ss1234", **kwargs):
    return User.objects.create_user(email=email, password=password, role=role, **kwargs)


def make_business(email="biz@test.com"):
    user = make_user(email, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user,
        legal_name="Test Co",
        contact_name="Owner",
        contact_phone="0599000000",
        verification_status="APPROVED",
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
        status=JobListingStatus.DRAFT,
        is_funded=False,
    )
    defaults.update(kwargs)
    return JobListing.objects.create(business=business_profile, **defaults)


class JobStateMachineTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.job = make_job(self.biz_profile)
        self.client.force_authenticate(user=self.biz_user)

    def test_draft_can_transition_to_pending_review(self):
        self.assertTrue(self.job.can_transition_to(JobListingStatus.PENDING_REVIEW))

    def test_draft_can_transition_to_cancelled(self):
        self.assertTrue(self.job.can_transition_to(JobListingStatus.CANCELLED))

    def test_draft_cannot_transition_to_published_directly(self):
        self.assertFalse(self.job.can_transition_to(JobListingStatus.PUBLISHED))

    def test_completed_job_has_no_valid_transitions(self):
        self.job.status = JobListingStatus.COMPLETED
        self.assertFalse(self.job.can_transition_to(JobListingStatus.CANCELLED))
        self.assertFalse(self.job.can_transition_to(JobListingStatus.IN_PROGRESS))

    def test_cancelled_job_has_no_valid_transitions(self):
        self.job.status = JobListingStatus.CANCELLED
        self.assertFalse(self.job.can_transition_to(JobListingStatus.DRAFT))
        self.assertFalse(self.job.can_transition_to(JobListingStatus.PUBLISHED))

    def test_publish_draft_job_returns_400(self):
        response = self.client.post(reverse("job-publish", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("Cannot publish", response.data["detail"])

    def test_publish_pending_review_job_succeeds(self):
        self.job.status = JobListingStatus.PENDING_REVIEW
        self.job.save(update_fields=["status"])
        response = self.client.post(reverse("job-publish", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.job.refresh_from_db()
        self.assertEqual(self.job.status, JobListingStatus.PUBLISHED)

    def test_cancel_published_job(self):
        self.job.status = JobListingStatus.PUBLISHED
        self.job.save(update_fields=["status"])
        response = self.client.post(reverse("job-cancel", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.job.refresh_from_db()
        self.assertEqual(self.job.status, JobListingStatus.CANCELLED)

    def test_cancel_cancelled_job_returns_400(self):
        self.job.status = JobListingStatus.CANCELLED
        self.job.save(update_fields=["status"])
        response = self.client.post(reverse("job-cancel", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_version_increments_on_update(self):
        self.job.status = JobListingStatus.DRAFT
        self.job.save()
        initial_version = self.job.version
        response = self.client.patch(
            reverse("job-detail", args=[self.job.pk]),
            {"title": "Updated Title", "description": "Updated"},
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.job.refresh_from_db()
        self.assertEqual(self.job.version, initial_version + 1)


class JobPublishPermissionTests(APITestCase):

    def test_unverified_business_cannot_publish(self):
        user = make_user("unverified@test.com", Role.BUSINESS)
        bp = BusinessProfile.objects.create(
            user=user, legal_name="Unverified Co",
            contact_name="Owner", contact_phone="0599000001",
            verification_status="PENDING",
        )
        self.client.force_authenticate(user=user)
        job = make_job(bp, status=JobListingStatus.PENDING_REVIEW)
        response = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_worker_cannot_publish_job(self):
        worker = make_worker("w@test.com")
        biz_user, biz_profile = make_business("b@test.com")
        job = make_job(biz_profile, status=JobListingStatus.PENDING_REVIEW)
        self.client.force_authenticate(user=worker)
        response = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class FundedJobPublishTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.funder = make_user("funder@test.com", Role.FUNDER)
        self.admin = make_user("admin@test.com", Role.ADMIN, is_staff=True)

    def test_funded_job_without_allocation_cannot_publish(self):
        job = make_job(self.biz_profile, is_funded=True, status=JobListingStatus.PENDING_REVIEW)
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("allocation", response.data["detail"].lower())

    def test_funded_job_with_active_allocation_can_publish(self):
        job = make_job(self.biz_profile, is_funded=True, status=JobListingStatus.PENDING_REVIEW)
        proposal = FundingProposal.objects.create(
            funder=self.funder, title="Gaza Fund", description="...",
            total_amount=100000, currency="ILS", status="APPROVED",
        )
        budget = FundingBudget.objects.create(
            proposal=proposal, total_amount=100000, currency="ILS"
        )
        FundingAllocation.objects.create(
            budget=budget, job_listing=job, amount=10000, currency="ILS",
            status="RESERVED",
        )
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(reverse("job-publish", args=[job.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)


class ApplicationTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile, status=JobListingStatus.PUBLISHED)

    def test_worker_can_apply_to_published_job(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("job-apply", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(
            JobApplication.objects.filter(job=self.job, worker=self.worker).exists()
        )

    def test_worker_cannot_apply_twice(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(reverse("job-apply", args=[self.job.pk]))
        response = self.client.post(reverse("job-apply", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_409_CONFLICT)
        self.assertEqual(
            JobApplication.objects.filter(job=self.job, worker=self.worker).count(), 1
        )

    def test_worker_cannot_apply_to_non_published_job(self):
        draft_job = make_job(self.biz_profile, status=JobListingStatus.DRAFT)
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("job-apply", args=[draft_job.pk]))
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_business_cannot_apply(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(reverse("job-apply", args=[self.job.pk]))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class ApplicationOfferTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job = make_job(self.biz_profile, status=JobListingStatus.PUBLISHED)
        self.app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.PENDING
        )

    def _offer_url(self):
        return reverse("application-action", args=[self.job.pk, self.app.pk])

    def test_offer_creates_assignment(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.patch(self._offer_url(), {"action": "offer"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(
            Assignment.objects.filter(job=self.job, worker=self.worker).exists()
        )

    def test_offer_sets_assignment_status_offered(self):
        self.client.force_authenticate(user=self.biz_user)
        self.client.patch(self._offer_url(), {"action": "offer"})
        assignment = Assignment.objects.get(job=self.job, worker=self.worker)
        self.assertEqual(assignment.status, AssignmentStatus.OFFERED)

    def test_offer_sets_application_status_offered(self):
        self.client.force_authenticate(user=self.biz_user)
        self.client.patch(self._offer_url(), {"action": "offer"})
        self.app.refresh_from_db()
        self.assertEqual(self.app.status, ApplicationStatus.OFFERED)

    def test_offer_twice_is_idempotent(self):
        self.client.force_authenticate(user=self.biz_user)
        self.client.patch(self._offer_url(), {"action": "offer"})
        # Re-offer after worker declined: reset app status manually for test
        self.app.status = ApplicationStatus.PENDING
        self.app.save(update_fields=["status"])
        assignment = Assignment.objects.get(job=self.job, worker=self.worker)
        assignment.status = AssignmentStatus.DECLINED
        assignment.save(update_fields=["status"])
        response = self.client.patch(self._offer_url(), {"action": "offer"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(
            Assignment.objects.filter(job=self.job, worker=self.worker).count(), 1
        )

    def test_cannot_offer_non_pending_application(self):
        self.app.status = ApplicationStatus.OFFERED
        self.app.save(update_fields=["status"])
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.patch(self._offer_url(), {"action": "offer"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_reject_application(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.patch(self._offer_url(), {"action": "reject"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.app.refresh_from_db()
        self.assertEqual(self.app.status, ApplicationStatus.REJECTED)

    def test_worker_can_withdraw_offered_application(self):
        self.app.status = ApplicationStatus.OFFERED
        self.app.save(update_fields=["status"])
        self.client.force_authenticate(user=self.worker)
        response = self.client.patch(self._offer_url(), {"action": "withdraw"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.app.refresh_from_db()
        self.assertEqual(self.app.status, ApplicationStatus.WITHDRAWN)

    def test_worker_cannot_use_offer_action(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.patch(self._offer_url(), {"action": "offer"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class ApplicationAssignmentIdTests(APITestCase):
    """assignment_id is present in JobApplicationSerializer response."""

    def setUp(self):
        from accounts.models import User, Role, BusinessProfile, WorkerProfile
        self.biz_user = User.objects.create_user(email="b@assignid.com", password="P@ss1234", role=Role.BUSINESS)
        bp = BusinessProfile.objects.create(
            user=self.biz_user, legal_name="AssignId Co", contact_name="Owner",
            contact_phone="0599000200", verification_status="APPROVED",
        )
        self.biz_user.is_verified = True
        self.biz_user.save(update_fields=["is_verified"])
        self.worker = User.objects.create_user(email="w@assignid.com", password="P@ss1234", role=Role.WORKER)
        WorkerProfile.objects.create(user=self.worker, full_name="Id Worker")
        import datetime
        self.job = JobListing.objects.create(
            business=bp, title="Test", description="Desc",
            location_address="Gaza", location_city="Gaza",
            work_date=datetime.date.today() + datetime.timedelta(days=1),
            start_time="08:00", end_time="17:00", num_workers_needed=1,
            wage_amount=5000, wage_currency="ILS", wage_basis="PER_DAY",
            payment_method="CASH", payment_deadline_days=3,
            status=JobListingStatus.PUBLISHED,
        )
        self.app = JobApplication.objects.create(
            job=self.job, worker=self.worker, status=ApplicationStatus.PENDING
        )

    def _list_url(self):
        return reverse("job-applications", args=[self.job.pk])

    def test_assignment_id_is_null_before_offer(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.get(self._list_url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        app_data = response.data["results"][0]
        self.assertIn("assignment_id", app_data)
        self.assertIsNone(app_data["assignment_id"])

    def test_assignment_id_populated_after_offer(self):
        from assignments.models import Assignment, AssignmentStatus
        assign = Assignment.objects.create(
            job=self.job, worker=self.worker, application=self.app,
            terms_snapshot={}, status=AssignmentStatus.OFFERED,
        )
        self.app.status = ApplicationStatus.OFFERED
        self.app.save(update_fields=["status"])
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.get(self._list_url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        app_data = response.data["results"][0]
        self.assertEqual(app_data["assignment_id"], str(assign.pk))

    def test_worker_name_and_phone_in_application_response(self):
        self.worker.phone = "0599123456"
        self.worker.save(update_fields=["phone"])
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.get(self._list_url())
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        app_data = response.data["results"][0]
        self.assertEqual(app_data["worker_name"], "Id Worker")
        self.assertEqual(app_data["worker_phone"], "0599123456")
