from django.test import TestCase, override_settings

from accounts.utils import normalize_national_id, hash_national_id
from accounts.models import User, Role, WorkerProfile


class NormalizeNationalIdTests(TestCase):

    def test_plain_nine_digits(self):
        self.assertEqual(normalize_national_id("123456789"), "123456789")

    def test_strips_whitespace(self):
        self.assertEqual(normalize_national_id("  123456789  "), "123456789")

    def test_strips_dashes(self):
        self.assertEqual(normalize_national_id("123-456-789"), "123456789")

    def test_strips_mixed_spaces_and_dashes(self):
        self.assertEqual(normalize_national_id(" 123 - 456789 "), "123456789")

    def test_rejects_eight_digits(self):
        with self.assertRaises(ValueError):
            normalize_national_id("12345678")

    def test_rejects_ten_digits(self):
        with self.assertRaises(ValueError):
            normalize_national_id("1234567890")

    def test_rejects_letters(self):
        with self.assertRaises(ValueError):
            normalize_national_id("12345678A")

    def test_rejects_empty_string(self):
        with self.assertRaises(ValueError):
            normalize_national_id("")


@override_settings(NATIONAL_ID_HMAC_KEY="test-hmac-key-32-bytes-long-xxxx")
class HashNationalIdTests(TestCase):

    def test_returns_64_hex_chars(self):
        result = hash_national_id("123456789")
        self.assertEqual(len(result), 64)
        self.assertRegex(result, r"^[0-9a-f]{64}$")

    def test_same_input_same_output(self):
        self.assertEqual(hash_national_id("123456789"), hash_national_id("123456789"))

    def test_normalizes_before_hashing(self):
        self.assertEqual(hash_national_id("123-456-789"), hash_national_id("123456789"))

    def test_different_input_different_output(self):
        self.assertNotEqual(hash_national_id("123456789"), hash_national_id("987654321"))

    @override_settings(NATIONAL_ID_HMAC_KEY="another-key-xxxxxxxxxxxxxxxxxxxxx")
    def test_different_key_different_output(self):
        h1 = hash_national_id("123456789")
        with self.settings(NATIONAL_ID_HMAC_KEY="different-key-xxxxxxxxxxxxxxxxxxx"):
            h2 = hash_national_id("123456789")
        self.assertNotEqual(h1, h2)

    @override_settings(NATIONAL_ID_HMAC_KEY="")
    def test_missing_key_raises(self):
        with self.assertRaises(ValueError):
            hash_national_id("123456789")


@override_settings(NATIONAL_ID_HMAC_KEY="test-hmac-key-32-bytes-long-xxxx")
class WorkerProfileSetNationalIdTests(TestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            phone="+970599050001", password="P@ss1234", role=Role.WORKER
        )
        self.profile = WorkerProfile.objects.create(user=self.user, full_name="Test Worker")

    def test_set_national_id_stores_hash(self):
        self.profile.set_national_id("123456789")
        self.assertEqual(len(self.profile.national_id_hash), 64)

    def test_set_national_id_is_deterministic(self):
        self.profile.set_national_id("123456789")
        first = self.profile.national_id_hash
        self.profile.set_national_id("123456789")
        self.assertEqual(self.profile.national_id_hash, first)

    def test_set_national_id_rejects_invalid(self):
        with self.assertRaises(ValueError):
            self.profile.set_national_id("bad-id")
