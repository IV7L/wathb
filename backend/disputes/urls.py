from django.urls import path
from disputes.views import (
    DisputeListCreateView, DisputeDetailView, DisputeEventListView,
    AdminDisputeAssignView, AdminDisputeResolveView,
)

urlpatterns = [
    path("", DisputeListCreateView.as_view(), name="dispute-list"),
    path("<uuid:pk>/", DisputeDetailView.as_view(), name="dispute-detail"),
    path("<uuid:pk>/events/", DisputeEventListView.as_view(), name="dispute-events"),
]

admin_urlpatterns = [
    path("disputes/<uuid:pk>/assign/", AdminDisputeAssignView.as_view(), name="admin-dispute-assign"),
    path("disputes/<uuid:pk>/resolve/", AdminDisputeResolveView.as_view(), name="admin-dispute-resolve"),
]
