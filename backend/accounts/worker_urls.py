from django.urls import path
from accounts.views import WorkerProfileView

urlpatterns = [
    path("profile/", WorkerProfileView.as_view(), name="worker-profile"),
]
