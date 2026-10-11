from rest_framework.permissions import BasePermission
from accounts.models import Role


class IsWorker(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and request.user.role == Role.WORKER)


class IsBusiness(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and request.user.role == Role.BUSINESS)


class IsFunder(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and request.user.role == Role.FUNDER)


class IsAdminRole(BasePermission):
    def has_permission(self, request, view):
        return bool(request.user and request.user.is_authenticated and request.user.role == Role.ADMIN)


class IsWorkerOrBusiness(BasePermission):
    def has_permission(self, request, view):
        return bool(
            request.user
            and request.user.is_authenticated
            and request.user.role in (Role.WORKER, Role.BUSINESS)
        )


class IsVerifiedBusiness(BasePermission):
    def has_permission(self, request, view):
        if not (request.user and request.user.is_authenticated and request.user.role == Role.BUSINESS):
            return False
        try:
            return request.user.business_profile.verification_status == "APPROVED"
        except Exception:
            return False
