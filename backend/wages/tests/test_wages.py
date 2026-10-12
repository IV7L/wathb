import datetime
import io

from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from accounts.models import User, Role, BusinessProfile, WorkerProfile
from assignments.models import Assignment, AssignmentStatus
from jobs.models import JobListing, JobListingStatus, JobApplication, ApplicationStatus
from wages.models import WageObligation, WageObligationStatus, PaymentDeclaration, PaymentDeclarationStatus


def make_user(phone, role, password="P@ss1234", **kwargs):
    return User.objects.create_user(phone, password, role=role, **kwargs)


def make_business(phone="+970599013001"):
    user = make_user(phone, Role.BUSINESS)
    bp = BusinessProfile.objects.create(
        user=user, legal_name="Wages Co", contact_name="Owner",
        contact_phone="0599000100", verification_status="APPROVED",
    )
    user.is_verified = True
    user.save(update_fields=["is_verified"])
    return user, bp


def make_worker(phone="+970599013002"):
    user = make_user(phone, Role.WORKER)
    WorkerProfile.objects.create(user=user, full_name="Wage Worker")
    return user


def make_job_and_assignment(biz_profile, worker_user, wage_amount=10000):
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
        wage_amount=wage_amount,
        wage_currency="ILS",
        wage_basis="PER_DAY",
        payment_method="CASH",
        payment_deadline_days=3,
        status=JobListingStatus.PUBLISHED,
    )
    app = JobApplication.objects.create(job=job, worker=worker_user, status=ApplicationStatus.ACCEPTED)
    assignment = Assignment.objects.create(
        job=job,
        worker=worker_user,
        application=app,
        terms_snapshot=job.material_terms_snapshot(),
        status=AssignmentStatus.COMPLETED,
    )
    return job, assignment


def make_obligation(assignment, wage_amount=10000, obligation_status=WageObligationStatus.PENDING):
    return WageObligation.objects.create(
        assignment=assignment,
        amount=wage_amount,
        currency="ILS",
        due_date=datetime.date.today() + datetime.timedelta(days=3),
        status=obligation_status,
    )


def make_declaration(obligation, declared_by, decl_status=PaymentDeclarationStatus.SUBMITTED):
    return PaymentDeclaration.objects.create(
        wage_obligation=obligation,
        declared_amount=obligation.amount,
        currency=obligation.currency,
        payment_date=datetime.date.today(),
        payment_method="CASH",
        declared_by=declared_by,
        status=decl_status,
    )


class WageObligationCreationTests(APITestCase):

    def test_wage_obligation_created_from_snapshot_values(self):
        biz_user, biz_profile = make_business("+970599013003")
        worker = make_worker("+970599013004")
        job, assignment = make_job_and_assignment(biz_profile, worker, wage_amount=15000)
        obligation = make_obligation(assignment, wage_amount=15000)
        self.assertEqual(obligation.amount, 15000)
        self.assertEqual(obligation.currency, "ILS")

    def test_only_one_obligation_per_assignment(self):
        biz_user, biz_profile = make_business("+970599013005")
        worker = make_worker("+970599013006")
        job, assignment = make_job_and_assignment(biz_profile, worker)
        make_obligation(assignment)
        from django.db import IntegrityError
        with self.assertRaises(IntegrityError):
            make_obligation(assignment)


class PaymentDeclarationTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.obligation = make_obligation(self.assignment)

    def _decl_body(self):
        return {
            "declared_amount": 10000,
            "currency": "ILS",
            "payment_date": str(datetime.date.today()),
            "payment_method": "CASH",
        }

    def test_business_can_declare_payment(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(
            reverse("wage-declare-payment", args=[self.obligation.pk]),
            self._decl_body(),
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.obligation.refresh_from_db()
        self.assertEqual(self.obligation.status, WageObligationStatus.AWAITING_ACK)

    def test_worker_cannot_declare_payment(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("wage-declare-payment", args=[self.obligation.pk]),
            self._decl_body(),
        )
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_cannot_declare_payment_for_already_declared_obligation(self):
        self.obligation.status = WageObligationStatus.AWAITING_ACK
        self.obligation.save(update_fields=["status"])
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(
            reverse("wage-declare-payment", args=[self.obligation.pk]),
            self._decl_body(),
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cannot_declare_for_another_businesss_obligation(self):
        other_biz_user, other_biz_profile = make_business("+970599013007")
        self.client.force_authenticate(user=other_biz_user)
        response = self.client.post(
            reverse("wage-declare-payment", args=[self.obligation.pk]),
            self._decl_body(),
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_invalid_payment_method_returns_400(self):
        """payment_method must be one of CASH / BANK_TRANSFER / OTHER."""
        self.client.force_authenticate(user=self.biz_user)
        body = self._decl_body()
        body["payment_method"] = "BITCOIN"
        response = self.client.post(
            reverse("wage-declare-payment", args=[self.obligation.pk]),
            body,
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_all_valid_payment_methods_accepted(self):
        for method in ("CASH", "BANK_TRANSFER", "OTHER"):
            # Re-create an obligation for each variant (tearDown not available here,
            # so we just chain a new obligation on a new assignment).
            _, assign = make_job_and_assignment(
                self.biz_profile, self.worker,
                wage_amount=5000 + list(("CASH", "BANK_TRANSFER", "OTHER")).index(method)
            )
            obl = make_obligation(assign)
            self.client.force_authenticate(user=self.biz_user)
            body = {
                "declared_amount": obl.amount,
                "currency": "ILS",
                "payment_date": str(__import__("datetime").date.today()),
                "payment_method": method,
            }
            resp = self.client.post(reverse("wage-declare-payment", args=[obl.pk]), body)
            self.assertEqual(resp.status_code, status.HTTP_201_CREATED, msg=f"method={method}")


class PaymentAcknowledgmentTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.obligation = make_obligation(self.assignment, obligation_status=WageObligationStatus.AWAITING_ACK)
        make_declaration(self.obligation, self.biz_user)

    def test_worker_can_acknowledge_payment(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("wage-acknowledge", args=[self.obligation.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.obligation.refresh_from_db()
        self.assertEqual(self.obligation.status, WageObligationStatus.VERIFIED_PAID)

    def test_acknowledgment_marks_declaration_acknowledged(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(reverse("wage-acknowledge", args=[self.obligation.pk]))
        decl = PaymentDeclaration.objects.get(wage_obligation=self.obligation)
        self.assertEqual(decl.status, PaymentDeclarationStatus.ACKNOWLEDGED)

    def test_cannot_acknowledge_non_awaiting_obligation(self):
        self.obligation.status = WageObligationStatus.PENDING
        self.obligation.save(update_fields=["status"])
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(reverse("wage-acknowledge", args=[self.obligation.pk]))
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_business_cannot_acknowledge_payment(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(reverse("wage-acknowledge", args=[self.obligation.pk]))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class PaymentDisputeTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business()
        self.worker = make_worker()
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.obligation = make_obligation(self.assignment, obligation_status=WageObligationStatus.AWAITING_ACK)
        make_declaration(self.obligation, self.biz_user)

    def test_worker_can_dispute_payment(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("wage-dispute-payment", args=[self.obligation.pk]),
            {"category": "PAYMENT_NOT_RECEIVED", "description": "I never got paid"},
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.obligation.refresh_from_db()
        self.assertEqual(self.obligation.status, WageObligationStatus.DISPUTED)

    def test_dispute_creates_dispute_record(self):
        from disputes.models import Dispute
        self.client.force_authenticate(user=self.worker)
        self.client.post(
            reverse("wage-dispute-payment", args=[self.obligation.pk]),
            {"category": "PAYMENT_NOT_RECEIVED", "description": "Not paid"},
        )
        self.assertTrue(Dispute.objects.filter(assignment=self.assignment).exists())

    def test_dispute_marks_declaration_disputed(self):
        self.client.force_authenticate(user=self.worker)
        self.client.post(
            reverse("wage-dispute-payment", args=[self.obligation.pk]),
            {"category": "PAYMENT_NOT_RECEIVED"},
        )
        decl = PaymentDeclaration.objects.get(wage_obligation=self.obligation)
        self.assertEqual(decl.status, PaymentDeclarationStatus.DISPUTED)

    def test_cannot_dispute_non_awaiting_obligation(self):
        self.obligation.status = WageObligationStatus.PENDING
        self.obligation.save(update_fields=["status"])
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(
            reverse("wage-dispute-payment", args=[self.obligation.pk]),
            {"category": "PAYMENT_NOT_RECEIVED"},
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_business_cannot_dispute_payment(self):
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(
            reverse("wage-dispute-payment", args=[self.obligation.pk]),
            {"category": "PAYMENT_NOT_RECEIVED"},
        )
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class FileUploadValidationTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business("+970599013010")
        self.worker = make_worker("+970599013011")
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.obligation = make_obligation(self.assignment, obligation_status=WageObligationStatus.AWAITING_ACK)
        self.declaration = make_declaration(self.obligation, self.biz_user)

    def _upload_url(self):
        return reverse("wage-upload-evidence", args=[self.obligation.pk, self.declaration.pk])

    def test_upload_valid_jpeg(self):
        jpeg_bytes = b'\xff\xd8\xff\xe0' + b'\x00' * 100
        f = io.BytesIO(jpeg_bytes)
        f.name = "receipt.jpg"
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(self._upload_url(), {"file": f}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_upload_valid_png(self):
        png_bytes = b'\x89PNG\r\n\x1a\n' + b'\x00' * 100
        f = io.BytesIO(png_bytes)
        f.name = "receipt.png"
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(self._upload_url(), {"file": f}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_upload_valid_pdf(self):
        pdf_bytes = b'%PDF' + b'\x00' * 100
        f = io.BytesIO(pdf_bytes)
        f.name = "receipt.pdf"
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(self._upload_url(), {"file": f}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_upload_disallowed_file_type_rejected(self):
        exe_bytes = b'MZ' + b'\x00' * 100
        f = io.BytesIO(exe_bytes)
        f.name = "malware.exe"
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(self._upload_url(), {"file": f}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("not allowed", response.data["detail"])

    def test_upload_oversized_file_rejected(self):
        from django.conf import settings
        large_bytes = b'\xff\xd8\xff\xe0' + b'\x00' * (settings.MAX_UPLOAD_SIZE_BYTES + 1)
        f = io.BytesIO(large_bytes)
        f.name = "large.jpg"
        self.client.force_authenticate(user=self.biz_user)
        response = self.client.post(self._upload_url(), {"file": f}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("size", response.data["detail"].lower())

    def test_worker_cannot_upload_evidence(self):
        f = io.BytesIO(b'\xff\xd8\xff\xe0' + b'\x00' * 100)
        f.name = "receipt.jpg"
        self.client.force_authenticate(user=self.worker)
        response = self.client.post(self._upload_url(), {"file": f}, format="multipart")
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)


class AdminVerifyPaymentTests(APITestCase):

    def setUp(self):
        self.biz_user, self.biz_profile = make_business("+970599013015")
        self.worker = make_worker("+970599013016")
        self.admin = make_user("+970599013017", Role.ADMIN)
        self.job, self.assignment = make_job_and_assignment(self.biz_profile, self.worker)
        self.obligation = make_obligation(self.assignment, obligation_status=WageObligationStatus.AWAITING_ACK)
        self.declaration = make_declaration(self.obligation, self.biz_user)

    def _verify_url(self):
        return reverse("wage-admin-verify", args=[self.obligation.pk, self.declaration.pk])

    def test_admin_can_verify_declaration(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(self._verify_url(), {"action": "verify"})
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.declaration.refresh_from_db()
        self.obligation.refresh_from_db()
        self.assertEqual(self.declaration.status, PaymentDeclarationStatus.VERIFIED)
        self.assertEqual(self.obligation.status, WageObligationStatus.VERIFIED_PAID)

    def test_admin_can_reject_with_notes(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(self._verify_url(), {"action": "reject", "review_notes": "Payment receipt unclear"})
        self.assertEqual(response.status_code, status.HTTP_200_OK, response.data)
        self.declaration.refresh_from_db()
        self.obligation.refresh_from_db()
        self.assertEqual(self.declaration.status, PaymentDeclarationStatus.REJECTED)
        self.assertEqual(self.obligation.status, WageObligationStatus.REJECTED)

    def test_rejection_requires_review_notes(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(self._verify_url(), {"action": "reject"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("review_notes", response.data["detail"])

    def test_non_admin_cannot_verify(self):
        for user in [self.biz_user, self.worker]:
            self.client.force_authenticate(user=user)
            response = self.client.post(self._verify_url(), {"action": "verify"})
            self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_invalid_action_returns_400(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.post(self._verify_url(), {"action": "approve"})
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_verify_sets_reviewed_by(self):
        self.client.force_authenticate(user=self.admin)
        self.client.post(self._verify_url(), {"action": "verify"})
        self.declaration.refresh_from_db()
        self.assertEqual(self.declaration.reviewed_by, self.admin)

    def test_wage_obligation_list_status_filter(self):
        other_biz_user, other_biz_profile = make_business("+970599013020")
        other_worker = make_worker("+970599013021")
        other_job, other_assignment = make_job_and_assignment(other_biz_profile, other_worker)
        make_obligation(other_assignment, obligation_status=WageObligationStatus.PENDING)

        self.client.force_authenticate(user=self.admin)
        response = self.client.get(reverse("wage-list"), {"status": "AWAITING_ACK"})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        statuses = [o["status"] for o in response.data["results"]]
        self.assertTrue(all(s == WageObligationStatus.AWAITING_ACK for s in statuses))

    def test_obligation_serializer_includes_declarations(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(reverse("wage-detail", args=[self.obligation.pk]))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("declarations", response.data)
        self.assertEqual(len(response.data["declarations"]), 1)
        self.assertEqual(response.data["declarations"][0]["id"], str(self.declaration.pk))
