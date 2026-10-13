from django.test import override_settings
from rest_framework.test import APITestCase


class AppConfigViewTest(APITestCase):
    URL = "/api/v1/app/config/"

    def test_returns_config_fields(self):
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        for field in ("min_supported_version_code", "latest_version_code",
                      "apk_download_url", "support_whatsapp"):
            self.assertIn(field, data)

    def test_no_auth_required(self):
        # Must succeed with no credentials at all.
        self.client.credentials()
        resp = self.client.get(self.URL)
        self.assertEqual(resp.status_code, 200)

    @override_settings(
        APP_MIN_SUPPORTED_VERSION_CODE=3,
        APP_LATEST_VERSION_CODE=5,
        APP_APK_DOWNLOAD_URL="https://example.com/wathb.apk",
        SUPPORT_WHATSAPP="+970599000000",
    )
    def test_values_from_settings(self):
        resp = self.client.get(self.URL)
        data = resp.json()
        self.assertEqual(data["min_supported_version_code"], 3)
        self.assertEqual(data["latest_version_code"], 5)
        self.assertEqual(data["apk_download_url"], "https://example.com/wathb.apk")
        self.assertEqual(data["support_whatsapp"], "+970599000000")
