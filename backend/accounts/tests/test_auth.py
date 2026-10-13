from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase
from accounts.models import User, Role, WorkerProfile, BusinessProfile


class RegistrationTests(APITestCase):

    def test_register_worker_success(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001001",
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "full_name": "Test Worker",
            "consent_version": 1,
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)
        self.assertEqual(response.data["user"]["role"], Role.WORKER)
        self.assertTrue(User.objects.filter(phone="+970599001001").exists())
        self.assertTrue(WorkerProfile.objects.filter(full_name="Test Worker").exists())

    def test_register_business_success(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001002",
            "password": "StrongPass123!",
            "role": Role.BUSINESS,
            "legal_name": "Test Biz",
            "contact_name": "Owner",
            "contact_phone": "0599001002",
            "consent_version": 1,
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(BusinessProfile.objects.filter(legal_name="Test Biz").exists())

    def test_register_duplicate_phone(self):
        User.objects.create_user("+970599001003", "P@ss1234", role=Role.WORKER)
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001003",
            "password": "P@ss1234",
            "role": Role.WORKER,
            "full_name": "Dup",
            "consent_version": 1,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_worker_missing_full_name(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001004",
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "consent_version": 1,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_worker_missing_phone(self):
        response = self.client.post(reverse("auth-register"), {
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "full_name": "No Phone",
            "consent_version": 1,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("phone", response.data.get("fields", response.data))

    def test_register_admin_not_allowed(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001005",
            "password": "StrongPass123!",
            "role": Role.ADMIN,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_funder_not_allowed(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001006",
            "password": "StrongPass123!",
            "role": Role.FUNDER,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_without_consent_version_returns_400(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001007",
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "full_name": "No Consent",
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_wrong_consent_version_returns_400(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001008",
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "full_name": "Wrong Consent",
            "consent_version": 999,
        })
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_register_stores_consent_version_and_accepted_at(self):
        response = self.client.post(reverse("auth-register"), {
            "phone": "0599001009",
            "password": "StrongPass123!",
            "role": Role.WORKER,
            "full_name": "Consenting Worker",
            "consent_version": 1,
        })
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        user = User.objects.get(phone="+970599001009")
        self.assertEqual(user.consent_version, 1)
        self.assertIsNotNone(user.accepted_at)


class LoginTests(APITestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            "+970599001010", "StrongPass123!", role=Role.WORKER,
        )

    def test_login_success(self):
        response = self.client.post(reverse("auth-login"), {
            "phone": "0599001010",
            "password": "StrongPass123!",
        })
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("access", response.data)
        self.assertIn("refresh", response.data)

    def test_login_wrong_password(self):
        response = self.client.post(reverse("auth-login"), {
            "phone": "0599001010",
            "password": "WrongPassword",
        })
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_login_nonexistent_user(self):
        response = self.client.post(reverse("auth-login"), {
            "phone": "0599001011",
            "password": "StrongPass123!",
        })
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_protected_endpoint_requires_token(self):
        response = self.client.get(reverse("auth-me"))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_protected_endpoint_with_valid_token(self):
        login_response = self.client.post(reverse("auth-login"), {
            "phone": "0599001010",
            "password": "StrongPass123!",
        })
        token = login_response.data["access"]
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
        response = self.client.get(reverse("auth-me"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["phone"], "+970599001010")


class RoleIsolationTests(APITestCase):

    def setUp(self):
        self.worker = User.objects.create_user(
            "+970599001021", "P@ss1234", role=Role.WORKER
        )
        self.business = User.objects.create_user(
            "+970599001022", "P@ss1234", role=Role.BUSINESS
        )
        self.funder = User.objects.create_user(
            "+970599001023", "P@ss1234", role=Role.FUNDER
        )
        self.admin = User.objects.create_user(
            "+970599001024", "P@ss1234", role=Role.ADMIN, is_staff=True
        )

    def _auth(self, user):
        response = self.client.post(reverse("auth-login"), {
            "phone": user.phone, "password": "P@ss1234"
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
            "+970599001020", "P@ss1234", role=Role.WORKER
        )
        WorkerProfile.objects.create(user=other_worker, full_name="Other")
        WorkerProfile.objects.create(user=self.worker, full_name="Mine")
        self._auth(self.worker)
        response = self.client.get(reverse("worker-profile"))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["full_name"], "Mine")


class PhoneNormalizationTests(APITestCase):

    def test_local_059x_format_normalized_on_login(self):
        User.objects.create_user("+970599001030", "P@ss1234", role=Role.WORKER)
        response = self.client.post(reverse("auth-login"), {
            "phone": "0599001030", "password": "P@ss1234"
        })
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_plus970_format_accepted_on_login(self):
        User.objects.create_user("+970599001031", "P@ss1234", role=Role.WORKER)
        response = self.client.post(reverse("auth-login"), {
            "phone": "+970599001031", "password": "P@ss1234"
        })
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_invalid_phone_returns_401(self):
        response = self.client.post(reverse("auth-login"), {
            "phone": "not-a-phone", "password": "P@ss1234"
        })
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class LogoutBlacklistTests(APITestCase):
    """
    E3: two distinct actions.
    - lock_app: client-side only (no server call) — refresh token stays valid.
    - logout: POST /auth/logout/ blacklists the refresh token; subsequent
      refresh attempts must return 401.
    """

    def setUp(self):
        self.user = User.objects.create_user(
            "+970599002020", "StrongPass123!", role=Role.WORKER,
        )
        login = self.client.post(reverse("auth-login"), {
            "phone": "0599002020", "password": "StrongPass123!",
        })
        self.access = login.data["access"]
        self.refresh = login.data["refresh"]

    def test_lock_app_refresh_token_still_valid(self):
        # lock_app is client-side only; from the server's view the refresh
        # token must still be usable after a lock (no blacklist call was made).
        response = self.client.post(reverse("auth-token-refresh"), {"refresh": self.refresh})
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("access", response.data)

    def test_logout_blacklists_refresh_token(self):
        # Full logout: POST /auth/logout/ with the refresh token.
        logout_response = self.client.post(
            reverse("auth-logout"), {"refresh": self.refresh}, format="json"
        )
        self.assertIn(logout_response.status_code, [
            status.HTTP_200_OK, status.HTTP_205_RESET_CONTENT
        ])

        # The blacklisted refresh token must no longer produce a new access token.
        refresh_response = self.client.post(
            reverse("auth-token-refresh"), {"refresh": self.refresh}, format="json"
        )
        self.assertEqual(refresh_response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_logout_without_refresh_token_returns_400(self):
        response = self.client.post(reverse("auth-logout"), {}, format="json")
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
