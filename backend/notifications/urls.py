from django.urls import path
from notifications.views import NotificationListView, NotificationMarkReadView, NotificationMarkAllReadView

urlpatterns = [
    path("", NotificationListView.as_view(), name="notification-list"),
    path("<uuid:pk>/read/", NotificationMarkReadView.as_view(), name="notification-read"),
    path("read-all/", NotificationMarkAllReadView.as_view(), name="notification-read-all"),
]
