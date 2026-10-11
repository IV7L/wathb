from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from accounts.models import User, Role, WorkerProfile, BusinessProfile


class RegistrationTests(APITestCase):

    def test_register_worker_success(self):
        response = self.client.post(reverse("auth-register"), {
            "email": "worker@test.com",
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "full_name": "Test Worker",
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)
        self.assertEqual(response.data["user"]["role"], Role.WORKER)
        self.assertTrue(User.objects.filter(email="worker@test.com").exists())
        self.assertTrue(WorkerProfile.objects.filter(full_name="Test Worker").exists())

    def test_register_business_success(self):
        response = self.client.post(reverse("auth-register"), {
            "email": "biz@test.com",
            "password": "StrongPass123!",
            "role": Role.BUSINESS,
            "legal_name": "Test Biz",
            "contact_name": "Owner",
            "contact_phone": "0599000000",
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(BusinessProfile.objects.filter(legal_name="Test Biz").exists())

    def test_register_duplicate_email(self):
        User.objects.create_user(email="dup@test.com", password="P@ss1234", role=Role.WORKER)
        response = self.client.post(reverse("auth-register"), {
            "email": "dup@test.com",
            "password": "P@ss1234",
            "role": Role.WORKER,
            "full_name": "Dup",
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_worker_missing_full_name(self):
        response = self.client.post(reverse("auth-register"), {
            "email": "noname@test.com",
            "password": "StrongPass123!",
            "role": Role.WORKER,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_admin_not_allowed(self):
        response = self.client.post(reverse("auth-register"), {
            "email": "admin@test.com",
            "password": "StrongPass123!",
            "role": Role.ADMIN,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_funder_not_allowed(self):
        response = self.client.post(reverse("auth-register"), {
            "email": "funder@test.com",
            "password": "StrongPass123!",
            "role": Role.FUNDER,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class LoginTests(APITestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            email="login@test.com",
            password="StrongPass123!",
            role=Role.WORKER,
        )

    def test_login_success(self):
        response = self.client.post(reverse("auth-login"), {
            "email": "login@test.com",
            "password": "StrongPass123!",
        })
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)

    def test_login_wrong_password(self):
        response = self.client.post(reverse("auth-login"), {
            "email": "login@test.com",
            "password": "WrongPassword",
        })
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_login_nonexistent_user(self):
        response = self.client.post(reverse("auth-login"), {
            "email": "notexist@test.com",
            "password": "StrongPass123!",
        })
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_protected_endpoint_requires_token(self):
        response = self.client.get(reverse("auth-me"))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_protected_endpoint_with_valid_token(self):
        login_response = self.client.post(reverse("auth-login"), {
            "email": "login@test.com",
            "password": "StrongPass123!",
        })
        token = login_response.data["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        response = self.client.get(reverse("auth-me"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["email"], "login@test.com")


class RoleIsolationTests(APITestCase):

    def setUp(self):
        self.worker = User.objects.create_user(
            email="worker_role@test.com", password="P@ss1234", role=Role.WORKER
        )
        self.business = User.objects.create_user(
            email="biz_role@test.com", password="P@ss1234", role=Role.BUSINESS
        )
        self.funder = User.objects.create_user(
            email="funder_role@test.com", password="P@ss1234", role=Role.FUNDER
        )
        self.admin = User.objects.create_user(
            email="admin_role@test.com", password="P@ss1234", role=Role.ADMIN, is_staff=True
        )

    def _auth(self, user):
        response = self.client.post(reverse("auth-login"), {
            "email": user.email, "password": "P@ss1234"
        })
        token = response.data["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")

    def test_worker_cannot_access_admin_business_list(self):
        self._auth(self.worker)
        response = self.client.get(reverse("admin-business-list"))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_business_cannot_access_admin_business_list(self):
        self._auth(self.business)
        response = self.client.get(reverse("admin-business-list"))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_funder_cannot_access_admin_business_list(self):
        self._auth(self.funder)
        response = self.client.get(reverse("admin-business-list"))
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_admin_can_access_admin_business_list(self):
        self._auth(self.admin)
        response = self.client.get(reverse("admin-business-list"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_worker_cannot_access_worker_profile_of_other_worker(self):
        other_worker = User.objects.create_user(
            email="other_worker@test.com", password="P@ss1234", role=Role.WORKER
        )
        WorkerProfile.objects.create(user=other_worker, full_name="Other")
        WorkerProfile.objects.create(user=self.worker, full_name="Mine")
        self._auth(self.worker)
        response = self.client.get(reverse("worker-profile"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["full_name"], "Mine")
