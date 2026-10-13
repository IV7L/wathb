from django.conf import settings
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView


class AppConfigView(APIView):
    permission_classes = [AllowAny]
    authentication_classes = []

    def get(self, request):
        return Response({
            "min_supported_version_code": settings.APP_MIN_SUPPORTED_VERSION_CODE,
            "latest_version_code": settings.APP_LATEST_VERSION_CODE,
            "apk_download_url": settings.APP_APK_DOWNLOAD_URL,
            "support_whatsapp": settings.SUPPORT_WHATSAPP,
        })
