from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from accounts.models import Role
from accounts.permissions import IsWorker, IsBusiness, IsAdminRole
from accounts.serializers import (
    RegisterSerializer,
    UserSerializer,
    WorkerProfileSerializer,
    BusinessProfileSerializer,
    AdminBusinessProfileSerializer,
)


class RegisterView(generics.CreateAPIView):
    permission_classes = [AllowAny]
    serializer_class = RegisterSerializer

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()
        refresh = RefreshToken.for_user(user)
        return Response(
            {
                "user": UserSerializer(user).data,
                "access": str(refresh.access_token),
                "refresh": str(refresh),
            },
            status=status.HTTP_201_CREATED,
        )


class MeView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        data = UserSerializer(user).data
        if user.role == Role.WORKER:
            try:
                data["profile"] = WorkerProfileSerializer(user.worker_profile).data
            except Exception:
                data["profile"] = None
        elif user.role == Role.BUSINESS:
            try:
                data["profile"] = BusinessProfileSerializer(user.business_profile).data
            except Exception:
                data["profile"] = None
        return Response(data)

    def patch(self, request):
        user = request.user
        allowed = {"phone"}
        updates = {k: v for k, v in request.data.items() if k in allowed}
        for field, value in updates.items():
            setattr(user, field, value)
        user.save(update_fields=list(updates.keys()))
        return Response(UserSerializer(user).data)


class WorkerProfileView(generics.RetrieveUpdateAPIView):
    permission_classes = [IsWorker]
    serializer_class = WorkerProfileSerializer

    def get_object(self):
        return self.request.user.worker_profile


class BusinessProfileView(generics.RetrieveUpdateAPIView):
    permission_classes = [IsBusiness]
    serializer_class = BusinessProfileSerializer

    def get_object(self):
        return self.request.user.business_profile


class AdminBusinessListView(generics.ListAPIView):
    permission_classes = [IsAdminRole]
    serializer_class = AdminBusinessProfileSerializer

    def get_queryset(self):
        from accounts.models import BusinessProfile
        qs = BusinessProfile.objects.select_related("user").order_by("-id")
        status_filter = self.request.query_params.get("verification_status")
        if status_filter:
            qs = qs.filter(verification_status=status_filter)
        return qs


class AdminBusinessVerifyView(APIView):
    permission_classes = [IsAdminRole]

    def patch(self, request, pk):
        from accounts.models import BusinessProfile
        try:
            bp = BusinessProfile.objects.select_related("user").get(pk=pk)
        except BusinessProfile.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        action = request.data.get("action")
        reason = request.data.get("reason", "")

        if action == "approve":
            bp.verification_status = "APPROVED"
            bp.verified_by = request.user
            bp.verified_at = timezone.now()
            bp.suspension_reason = ""
            bp.save()
            bp.user.is_verified = True
            bp.user.save(update_fields=["is_verified"])
            _log_audit(request.user, "business_approved", "BusinessProfile", str(bp.pk))
        elif action == "reject":
            bp.verification_status = "REJECTED"
            bp.suspension_reason = reason
            bp.save()
            _log_audit(request.user, "business_rejected", "BusinessProfile", str(bp.pk), {"reason": reason})
        elif action == "suspend":
            if not reason:
                return Response({"detail": "Reason required for suspension."}, status=status.HTTP_400_BAD_REQUEST)
            bp.verification_status = "SUSPENDED"
            bp.suspension_reason = reason
            bp.save()
            bp.user.is_verified = False
            bp.user.save(update_fields=["is_verified"])
            _log_audit(request.user, "business_suspended", "BusinessProfile", str(bp.pk), {"reason": reason})
        else:
            return Response({"detail": "action must be approve, reject, or suspend."}, status=status.HTTP_400_BAD_REQUEST)

        return Response(AdminBusinessProfileSerializer(bp).data)


def _log_audit(actor, action, model_name, object_id, details=None):
    try:
        from audit.models import AuditEvent
        AuditEvent.objects.create(
            actor=actor,
            action=action,
            model_name=model_name,
            object_id=object_id,
            details=details or {},
        )
    except Exception:
        pass
