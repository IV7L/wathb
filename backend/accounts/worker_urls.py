from django.urls import path
from accounts.views import WorkerProfileView
from jobs.views import WorkerApplicationListView

urlpatterns = [
    path("profile/", WorkerProfileView.as_view(), name="worker-profile"),
    path("applications/", WorkerApplicationListView.as_view(), name="worker-applications"),
]
