from .base import *  # noqa: F401, F403

DEBUG = False
ALLOWED_HOSTS = ["localhost", "testserver"]

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
