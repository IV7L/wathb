"""
ensure_admin — idempotent management command.

Reads numbered env vars ADMIN_1_PHONE / ADMIN_1_PASSWORD / ADMIN_1_EMAIL …
for i = 1, 2, … until ADMIN_i_PHONE is not set.
For each entry: get_or_create by phone, then set / update password and email.
Safe to run repeatedly (e.g. in build.sh on every deploy).
"""
import os

from django.core.management.base import BaseCommand

from accounts.models import Role, User


class Command(BaseCommand):
    help = "Create or update ADMIN accounts from numbered env vars (ADMIN_1_PHONE, …)."

    def handle(self, *args, **options):
        created_count = 0
        updated_count = 0
        i = 1
        while True:
            phone = os.environ.get(f"ADMIN_{i}_PHONE", "").strip()
            if not phone:
                break

            password = os.environ.get(f"ADMIN_{i}_PASSWORD", "").strip()
            email = os.environ.get(f"ADMIN_{i}_EMAIL", "").strip() or f"admin{i}@wathb.local"

            if not password:
                self.stderr.write(
                    self.style.WARNING(f"ADMIN_{i}_PASSWORD not set — skipping admin {i}.")
                )
                i += 1
                continue

            user, created = User.objects.get_or_create(
                phone=phone,
                defaults={
                    "email": email,
                    "role": Role.ADMIN,
                    "is_staff": True,
                    "is_superuser": True,
                    "is_verified": True,
                },
            )
            user.set_password(password)
            if not created:
                user.email = email
                user.role = Role.ADMIN
                user.is_staff = True
                user.is_superuser = True
                user.is_verified = True
            user.save()

            if created:
                created_count += 1
                self.stdout.write(self.style.SUCCESS(f"Created admin: {phone}"))
            else:
                updated_count += 1
                self.stdout.write(self.style.SUCCESS(f"Updated admin: {phone}"))

            i += 1

        if created_count == 0 and updated_count == 0 and i == 1:
            self.stdout.write("No ADMIN_1_PHONE env var found — no admins configured.")
        else:
            self.stdout.write(
                f"ensure_admin: {created_count} created, {updated_count} updated."
            )
