from django.urls import path
from jobs.views import (
    JobListView, JobDetailView, JobSubmitView, JobPublishView, JobCancelView,
    JobApplicationListView, JobApplyView, ApplicationActionView,
)

urlpatterns = [
    path("", JobListView.as_view(), name="job-list"),
    path("<uuid:pk>/", JobDetailView.as_view(), name="job-detail"),
    path("<uuid:pk>/submit/", JobSubmitView.as_view(), name="job-submit"),
    path("<uuid:pk>/publish/", JobPublishView.as_view(), name="job-publish"),
    path("<uuid:pk>/cancel/", JobCancelView.as_view(), name="job-cancel"),
    path("<uuid:pk>/applications/", JobApplicationListView.as_view(), name="job-applications"),
    path("<uuid:pk>/apply/", JobApplyView.as_view(), name="job-apply"),
    path("<uuid:pk>/applications/<uuid:app_id>/", ApplicationActionView.as_view(), name="application-action"),
]
