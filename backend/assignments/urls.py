from django.urls import path
from assignments.views import (
    AssignmentListView, AssignmentDetailView,
    AssignmentAcceptView, AssignmentDeclineView, AssignmentCancelView,
    AttendanceListCreateView, AttendanceConfirmView, AttendanceDisputeView,
    CompletionView, CompletionConfirmView, CompletionDisputeView,
    BusinessCompleteAssignmentView,
)

urlpatterns = [
    path("", AssignmentListView.as_view(), name="assignment-list"),
    path("<uuid:pk>/", AssignmentDetailView.as_view(), name="assignment-detail"),
    path("<uuid:pk>/accept/", AssignmentAcceptView.as_view(), name="assignment-accept"),
    path("<uuid:pk>/decline/", AssignmentDeclineView.as_view(), name="assignment-decline"),
    path("<uuid:pk>/cancel/", AssignmentCancelView.as_view(), name="assignment-cancel"),
    path("<uuid:pk>/attendance/", AttendanceListCreateView.as_view(), name="attendance-list"),
    path("<uuid:pk>/attendance/<uuid:rec_id>/confirm/", AttendanceConfirmView.as_view(), name="attendance-confirm"),
    path("<uuid:pk>/attendance/<uuid:rec_id>/dispute/", AttendanceDisputeView.as_view(), name="attendance-dispute"),
    path("<uuid:pk>/completion/", CompletionView.as_view(), name="completion"),
    path("<uuid:pk>/completion/confirm/", CompletionConfirmView.as_view(), name="completion-confirm"),
    path("<uuid:pk>/completion/dispute/", CompletionDisputeView.as_view(), name="completion-dispute"),
    path("<uuid:pk>/business-complete/", BusinessCompleteAssignmentView.as_view(), name="business-complete-assignment"),
]
