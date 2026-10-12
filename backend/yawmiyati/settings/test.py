from .base import *  # noqa: F401, F403

DEBUG = False
ALLOWED_HOSTS = ["localhost", "testserver"]

# Disable throttling in tests — override DEFAULT_THROTTLE_RATES to very high values
# (PhoneTokenObtainPairView uses explicit throttle_classes so we also override the cache key)
REST_FRAMEWORK["DEFAULT_THROTTLE_CLASSES"] = []
REST_FRAMEWORK["DEFAULT_THROTTLE_RATES"] = {
    "login_per_ip": "1000/min",
    "login_per_phone": "1000/min",
}

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": "yawmiyati_test",
        "USER": "macbook",
        "PASSWORD": "",
        "HOST": "localhost",
        "PORT": "5432",
    }
}

PASSWORD_HASHERS = [
    "django.contrib.auth.hashers.MD5PasswordHasher",
]

MEDIA_ROOT = "/tmp/yawmiyati_test_media/"
