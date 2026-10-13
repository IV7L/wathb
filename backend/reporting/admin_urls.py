from django.urls import path, include
from reporting.views import PilotSummaryView, PilotSummaryExportView
from accounts.views import AdminBusinessListView, AdminBusinessVerifyView, AdminWorkerListView, AdminWorkerVerifyView
from audit.views import AuditLogView
from disputes.urls import admin_urlpatterns as dispute_admin_urls
from funding.urls import admin_urlpatterns as funding_admin_urls
from wages.views import AdminStaleWageListView

urlpatterns = [
    path("businesses/", AdminBusinessListView.as_view(), name="admin-business-list"),
    path("businesses/<int:pk>/verify/", AdminBusinessVerifyView.as_view(), name="admin-business-verify"),
    path("workers/", AdminWorkerListView.as_view(), name="admin-worker-list"),
    path("workers/<int:pk>/verify/", AdminWorkerVerifyView.as_view(), name="admin-worker-verify"),
    path("audit-log/", AuditLogView.as_view(), name="admin-audit-log"),
    path("reports/pilot-summary/", PilotSummaryView.as_view(), name="admin-pilot-summary"),
    path("reports/pilot-summary/export/", PilotSummaryExportView.as_view(), name="admin-pilot-summary-export"),
    path("disputes/stale-wages/", AdminStaleWageListView.as_view(), name="admin-stale-wages"),
] + dispute_admin_urls + funding_admin_urls
