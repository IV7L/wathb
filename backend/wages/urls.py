from django.urls import path
from wages.views import (
    WageObligationListView, WageObligationDetailView,
    DeclarePaymentView, AcknowledgePaymentView, DisputePaymentView,
    UploadEvidenceView, EvidenceFileView, AdminVerifyPaymentView,
)

urlpatterns = [
    path("", WageObligationListView.as_view(), name="wage-list"),
    path("<uuid:pk>/", WageObligationDetailView.as_view(), name="wage-detail"),
    path("<uuid:pk>/declare-payment/", DeclarePaymentView.as_view(), name="wage-declare-payment"),
    path("<uuid:pk>/acknowledge/", AcknowledgePaymentView.as_view(), name="wage-acknowledge"),
    path("<uuid:pk>/dispute-payment/", DisputePaymentView.as_view(), name="wage-dispute-payment"),
    path("<uuid:pk>/declarations/<uuid:dec_id>/evidence/", UploadEvidenceView.as_view(), name="wage-upload-evidence"),
    path("<uuid:pk>/declarations/<uuid:dec_id>/evidence/<uuid:ev_id>/file/", EvidenceFileView.as_view(), name="wage-evidence-file"),
    path("<uuid:pk>/declarations/<uuid:dec_id>/verify/", AdminVerifyPaymentView.as_view(), name="wage-admin-verify"),
]
