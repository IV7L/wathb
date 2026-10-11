from .base import *  # noqa: F401, F403
from decouple import config, Csv

DEBUG = False

ALLOWED_HOSTS = config("DJANGO_ALLOWED_HOSTS", cast=Csv())

# HTTPS security headers
SECURE_HSTS_SECONDS             = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS  = True
SECURE_HSTS_PRELOAD             = True
SECURE_SSL_REDIRECT             = True
SECURE_PROXY_SSL_HEADER         = ("HTTP_X_FORWARDED_PROTO", "https")
SESSION_COOKIE_SECURE           = True
CSRF_COOKIE_SECURE              = True
SECURE_CONTENT_TYPE_NOSNIFF     = True
X_FRAME_OPTIONS                 = "DENY"

# CORS — no wildcard in production
CORS_ALLOW_ALL_ORIGINS  = False
CORS_ALLOWED_ORIGINS    = config("CORS_ALLOWED_ORIGINS", cast=Csv())

# Static file collection path
STATIC_ROOT = config("STATIC_ROOT", default=str(BASE_DIR / "staticfiles"))
