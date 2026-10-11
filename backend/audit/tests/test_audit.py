from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APITestCase

from accounts.models import User, Role
from audit.models import AuditEvent


def make_user(email, role, password="P@ss1234", **kwargs):
    return User.objects.create_user(email=email, password=password, role=role, **kwargs)


class AuditEventImmutabilityTests(TestCase):

    def setUp(self):
        self.actor = make_user("actor@audit.com", Role.ADMIN, is_staff=True)
        self.event = AuditEvent.objects.create(
            actor=self.actor,
            action="test_action",
            model_name="TestModel",
            object_id="123",
            details={"key": "value"},
        )

    def test_audit_event_creation_succeeds(self):
        self.assertIsNotNone(self.event.pk)
        self.assertEqual(self.event.action, "test_action")

    def test_audit_event_cannot_be_modified(self):
        with self.assertRaises(ValueError) as ctx:
            self.event.action = "modified_action"
            self.event.save()
        self.assertIn("immutable", str(ctx.exception).lower())

    def test_audit_event_cannot_be_deleted(self):
        with self.assertRaises(ValueError) as ctx:
            self.event.delete()
        self.assertIn("cannot be deleted", str(ctx.exception).lower())

    def test_multiple_audit_events_can_be_created(self):
        AuditEvent.objects.create(
            actor=self.actor,
            action="another_action",
            model_name="AnotherModel",
            object_id="456",
        )
        self.assertEqual(AuditEvent.objects.filter(actor=self.actor).count(), 2)

    def test_audit_event_ordering_newest_first(self):
        import time
        AuditEvent.objects.create(
            actor=self.actor, action="first_action",
            model_name="Model", object_id="1",
        )
        AuditEvent.objects.create(
            actor=self.actor, action="second_action",
            model_name="Model", object_id="2",
        )
        events = AuditEvent.objects.filter(actor=self.actor).order_by("-timestamp")
        actions = [e.action for e in events]
        self.assertIn("second_action", actions)
        self.assertEqual(actions[0], "second_action")

    def test_audit_event_actor_can_be_null(self):
        event = AuditEvent.objects.create(
            actor=None,
            action="system_action",
            model_name="System",
            object_id="0",
        )
        self.assertIsNone(event.actor)


class AuditLogAPITests(APITestCase):

    def setUp(self):
        self.admin = make_user("admin@auditapi.com", Role.ADMIN, is_staff=True)
        self.worker = make_user("worker@auditapi.com", Role.WORKER)
        AuditEvent.objects.create(
            actor=self.admin, action="business_approved",
            model_name="BusinessProfile", object_id="1",
        )

    def test_admin_can_access_audit_log(self):
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(reverse("admin-audit-log"))
        self.assertEqual(response.status_code, 200)

    def test_worker_cannot_access_audit_log(self):
        self.client.force_authenticate(user=self.worker)
        response = self.client.get(reverse("admin-audit-log"))
        self.assertEqual(response.status_code, 403)

    def test_unauthenticated_cannot_access_audit_log(self):
        response = self.client.get(reverse("admin-audit-log"))
        self.assertEqual(response.status_code, 401)
