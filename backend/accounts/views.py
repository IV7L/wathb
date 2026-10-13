import hashlib

from django.db.models import Q
from django.utils import timezone
from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.throttling import SimpleRateThrottle
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView

from accounts.models import Role, WorkerProfile
from accounts.permissions import IsWorker, IsBusiness, IsAdminRole
from accounts.serializers import (
    PhoneTokenObtainPairSerializer,
    RegisterSerializer,
    UserSerializer,
    WorkerProfileSerializer,
    AdminWorkerProfileSerializer,
    BusinessProfileSerializer,
    AdminBusinessProfileSerializer,
)


class _LoginPerIPThrottle(SimpleRateThrottle):
    scope = "login_per_ip"

    def get_cache_key(self, request, view):
        ident = self.get_ident(request)
        return self.cache_format % {"scope": self.scope, "ident": ident}


class _LoginPerPhoneThrottle(SimpleRateThrottle):
    scope = "login_per_phone"

    def get_cache_key(self, request, view):
        phone = (request.data or {}).get("phone", "")
        if not phone:
            return None
        ident = hashlib.sha256(phone.encode()).hexdigest()[:32]
        return self.cache_format % {"scope": self.scope, "ident": ident}


class PhoneTokenObtainPairView(TokenObtainPairView):
    serializer_class = PhoneTokenObtainPairSerializer
    throttle_classes = [_LoginPerIPThrottle, _LoginPerPhoneThrottle]

    def get_throttles(self):
        from django.conf import settings
        # When tests set DEFAULT_THROTTLE_CLASSES = [] we honour that and skip throttling.
        drf_cfg = getattr(settings, "REST_FRAMEWORK", {})
        if drf_cfg.get("DEFAULT_THROTTLE_CLASSES") == []:
            return []
        return super().get_throttles()


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
        from accounts.utils import normalize_phone
        user = request.user
        allowed = {"phone"}
        updates = {k: v for k, v in request.data.items() if k in allowed}
        if "phone" in updates:
            try:
                updates["phone"] = normalize_phone(updates["phone"])
            except ValueError as exc:
                return Response({"phone": str(exc)}, status=status.HTTP_400_BAD_REQUEST)
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


class AdminWorkerListView(generics.ListAPIView):
    permission_classes = [IsAdminRole]
    serializer_class = AdminWorkerProfileSerializer

    def get_queryset(self):
        qs = WorkerProfile.objects.select_related("user").order_by("-id")
        is_verified = self.request.query_params.get("is_verified")
        if is_verified is not None:
            qs = qs.filter(user__is_verified=is_verified.lower() == "true")
        search = self.request.query_params.get("search", "").strip()
        if search:
            qs = qs.filter(
                Q(user__phone__icontains=search) |
                Q(full_name__icontains=search)
            )
        return qs


class AdminWorkerVerifyView(APIView):
    permission_classes = [IsAdminRole]

    def patch(self, request, pk):
        try:
            wp = WorkerProfile.objects.select_related("user").get(pk=pk)
        except WorkerProfile.DoesNotExist:
            return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

        action = request.data.get("action")

        if action == "verify":
            wp.user.is_verified = True
            wp.user.save(update_fields=["is_verified"])
            _log_audit(request.user, "worker_verified", "WorkerProfile", str(wp.pk))
        elif action == "suspend":
            wp.user.is_verified = False
            wp.user.save(update_fields=["is_verified"])
            _log_audit(request.user, "worker_suspended", "WorkerProfile", str(wp.pk))
        else:
            return Response(
                {"detail": "action must be verify or suspend."},
                status=status.HTTP_400_BAD_REQUEST
            )

        return Response(AdminWorkerProfileSerializer(wp).data)


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
