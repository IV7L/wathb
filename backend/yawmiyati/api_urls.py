from django.urls import path, include

urlpatterns = [
    path("health/", include("yawmiyati.health_urls")),
    path("auth/", include("accounts.auth_urls")),
    path("workers/", include("accounts.worker_urls")),
    path("businesses/", include("businesses.urls")),
    path("jobs/", include("jobs.urls")),
    path("assignments/", include("assignments.urls")),
    path("wages/", include("wages.urls")),
    path("disputes/", include("disputes.urls")),
    path("funding/", include("funding.urls")),
    path("notifications/", include("notifications.urls")),
    path("admin/", include("reporting.admin_urls")),
]
