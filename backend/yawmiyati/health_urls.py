import logging

from django.urls import path
from django.http import JsonResponse
from django.db import connection

logger = logging.getLogger(__name__)


def health(request):
    try:
        connection.ensure_connection()
        db_ok = True
    except Exception as exc:
        # Log exception class and message for Render log diagnosis.
        # psycopg2 error strings contain host/user info but never passwords.
        logger.error("DB health-check failed [%s]: %s", type(exc).__name__, str(exc)[:400])
        db_ok = False
    status_code = 200 if db_ok else 503
    return JsonResponse(
        {"status": "ok" if db_ok else "degraded", "db": db_ok},
        status=status_code,
    )


urlpatterns = [
    path("", health, name="health"),
]
