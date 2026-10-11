from django.urls import path
from funding.views import (
    FundingProposalListCreateView, FundingProposalDetailView, FundingBudgetView,
    AdminApproveFundingView, AdminRejectFundingView, AdminAllocateFundingView,
)

urlpatterns = [
    path("proposals/", FundingProposalListCreateView.as_view(), name="funding-proposal-list"),
    path("proposals/<uuid:pk>/", FundingProposalDetailView.as_view(), name="funding-proposal-detail"),
    path("proposals/<uuid:pk>/budget/", FundingBudgetView.as_view(), name="funding-budget"),
]

admin_urlpatterns = [
    path("funding/proposals/<uuid:pk>/approve/", AdminApproveFundingView.as_view(), name="admin-funding-approve"),
    path("funding/proposals/<uuid:pk>/reject/", AdminRejectFundingView.as_view(), name="admin-funding-reject"),
    path("funding/proposals/<uuid:pk>/allocate/", AdminAllocateFundingView.as_view(), name="admin-funding-allocate"),
]
