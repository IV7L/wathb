from django.urls import path
from audit.views import AuditLogView

urlpatterns = [
    path("", AuditLogView.as_view(), name="audit-log"),
]
