from accounts.models import User


class PhoneAuthBackend:
    """Authenticate users by phone number (normalized to E.164)."""

    def authenticate(self, request, phone=None, password=None, **kwargs):
        if not phone or not password:
            return None
        from accounts.utils import normalize_phone
        try:
            normalized = normalize_phone(phone)
        except ValueError:
            return None
        try:
            user = User.objects.get(phone=normalized)
        except User.DoesNotExist:
            return None
        if user.check_password(password) and self._can_authenticate(user):
            return user
        return None

    def get_user(self, user_id):
        try:
            return User.objects.get(pk=user_id)
        except User.DoesNotExist:
            return None

    def _can_authenticate(self, user):
        return getattr(user, "is_active", False)
