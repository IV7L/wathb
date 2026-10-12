import logging

from django.urls import path
from django.http import JsonResponse
from django.db import connection
from django.db.migrations.executor import MigrationExecutor

logger = logging.getLogger(__name__)


def health(request):
    db_ok = False
    migrations_status = "unknown"

    try:
        # Real query — ensures the DB is queryable, not just reachable.
        with connection.cursor() as cursor:
            cursor.execute("SELECT 1")
        db_ok = True
    except Exception as exc:
        logger.error("DB health-check failed [%s]: %s", type(exc).__name__, str(exc)[:400])

    if db_ok:
        try:
            executor = MigrationExecutor(connection)
            plan = executor.migration_plan(executor.loader.graph.leaf_nodes())
            migrations_status = "ok" if not plan else "pending"
        except Exception as exc:
            logger.error("Migration check failed [%s]: %s", type(exc).__name__, str(exc)[:400])
            migrations_status = "unknown"

    healthy = db_ok and migrations_status == "ok"
    return JsonResponse(
        {
            "status": "ok" if healthy else "degraded",
            "db": db_ok,
            "migrations": migrations_status,
        },
        status=200 if healthy else 503,
    )


urlpatterns = [
    path("", health, name="health"),
]
