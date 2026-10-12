from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView, TokenBlacklistView
from accounts.views import PhoneTokenObtainPairView, RegisterView, MeView

urlpatterns = [
    path("register/", RegisterView.as_view(), name="auth-register"),
    path("login/", PhoneTokenObtainPairView.as_view(), name="auth-login"),
    path("token/refresh/", TokenRefreshView.as_view(), name="auth-token-refresh"),
    path("logout/", TokenBlacklistView.as_view(), name="auth-logout"),
    path("me/", MeView.as_view(), name="auth-me"),
]
