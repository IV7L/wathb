from django.urls import path
from accounts.views import BusinessProfileView

urlpatterns = [
    path("profile/", BusinessProfileView.as_view(), name="business-profile"),
]
