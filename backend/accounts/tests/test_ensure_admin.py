from io import StringIO
from unittest import mock

from django.core.management import call_command
from django.test import TestCase

from accounts.models import Role, User


class EnsureAdminCommandTest(TestCase):

    def _call(self, env):
        out = StringIO()
        with mock.patch.dict("os.environ", env, clear=False):
            call_command("ensure_admin", stdout=out, stderr=StringIO())
        return out.getvalue()

    def test_no_env_vars_is_a_no_op(self):
        out = self._call({})
        self.assertIn("no admins configured", out)
        self.assertEqual(User.objects.filter(role=Role.ADMIN).count(), 0)

    def test_creates_admin_from_env(self):
        out = self._call({
            "ADMIN_1_PHONE": "+970599000002",
            "ADMIN_1_PASSWORD": "Str0ng!",
            "ADMIN_1_EMAIL": "adm@test.ps",
        })
        self.assertIn("Created admin", out)
        user = User.objects.get(phone="+970599000002")
        self.assertEqual(user.role, Role.ADMIN)
        self.assertTrue(user.is_staff)
        self.assertTrue(user.is_superuser)
        self.assertTrue(user.check_password("Str0ng!"))

    def test_idempotent_update(self):
        env = {
            "ADMIN_1_PHONE": "+970599000003",
            "ADMIN_1_PASSWORD": "Pass1!",
            "ADMIN_1_EMAIL": "a@test.ps",
        }
        self._call(env)
        env["ADMIN_1_PASSWORD"] = "Pass2!"
        out = self._call(env)
        self.assertIn("Updated admin", out)
        user = User.objects.get(phone="+970599000003")
        self.assertTrue(user.check_password("Pass2!"))

    def test_supports_multiple_admins(self):
        self._call({
            "ADMIN_1_PHONE": "+970599000004",
            "ADMIN_1_PASSWORD": "Aa1!xxxx",
            "ADMIN_2_PHONE": "+970599000005",
            "ADMIN_2_PASSWORD": "Bb2!xxxx",
        })
        self.assertEqual(
            User.objects.filter(role=Role.ADMIN, phone__in=["+970599000004", "+970599000005"]).count(),
            2,
        )

    def test_skips_entry_without_password(self):
        err = StringIO()
        with mock.patch.dict("os.environ", {"ADMIN_1_PHONE": "+970599000006"}, clear=False):
            call_command("ensure_admin", stdout=StringIO(), stderr=err)
        self.assertFalse(User.objects.filter(phone="+970599000006").exists())
        self.assertIn("not set", err.getvalue())

    def test_invalid_phone_is_skipped(self):
        err = StringIO()
        with mock.patch.dict("os.environ", {
            "ADMIN_1_PHONE": "not-a-valid-phone",
            "ADMIN_1_PASSWORD": "Pass1!",
        }, clear=False):
            call_command("ensure_admin", stdout=StringIO(), stderr=err)
        self.assertEqual(User.objects.filter(role=Role.ADMIN).count(), 0)
        self.assertIn("invalid", err.getvalue().lower())
