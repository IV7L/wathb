# Wathb — Pilot Setup Guide

This guide walks through setting up the Wathb backend and Android app for a live pilot deployment in Gaza.

---

## Prerequisites

| Requirement | Notes |
|-------------|-------|
| Ubuntu 22.04 LTS server | 2 vCPU, 2 GB RAM minimum |
| PostgreSQL 14+ | Local install or managed service |
| Python 3.12 | Via pyenv or system package |
| Android device | API 26+ (Android 8.0+) |
| ADB (for sideloading) | During pilot; replace with internal track later |

---

## Part 1: Server Setup

### 1.1 Clone and Install

```bash
git clone https://github.com/your-org/yawmiyati.git
cd yawmiyati/backend

python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 1.2 Environment

```bash
cp .env.example .env
# Edit .env — fill every value. See comments in .env.example.
# Critical: DJANGO_SECRET_KEY must be random, DJANGO_DEBUG=False,
#           DJANGO_SETTINGS_MODULE=yawmiyati.settings.production
```

Generate a secret key:
```bash
python -c "import secrets; print(secrets.token_hex(50))"
```

### 1.3 Database

```bash
# Create the database and user (run as postgres superuser)
sudo -u postgres psql -c "CREATE USER wathb_user WITH PASSWORD 'your-password';"
sudo -u postgres psql -c "CREATE DATABASE wathb_prod OWNER wathb_user;"

# Apply migrations (from backend/ with .venv active)
python manage.py migrate --settings=yawmiyati.settings.production

# Create admin superuser
python manage.py createsuperuser --settings=yawmiyati.settings.production
```

### 1.4 Static Files

```bash
python manage.py collectstatic --settings=yawmiyati.settings.production --noinput
```

### 1.5 Run Deploy Check

```bash
python manage.py check --deploy --settings=yawmiyati.settings.production
```

All checks must pass (or be explicitly waived with documented justification).

### 1.6 Start with Gunicorn

```bash
gunicorn yawmiyati.wsgi:application \
  --workers 3 \
  --bind 0.0.0.0:8000 \
  --access-logfile - \
  --error-logfile -
```

For production, run behind nginx with TLS. See nginx config example below.

### 1.7 Nginx Configuration (minimal)

```nginx
server {
    listen 443 ssl;
    server_name api.wathb.example.com;

    ssl_certificate     /etc/letsencrypt/live/api.wathb.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.wathb.example.com/privkey.pem;

    location /api/ {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto https;
    }

    location /static/ {
        alias /opt/wathb/backend/staticfiles/;
    }

    location /media/ {
        alias /opt/wathb/backend/media/;
    }
}
```

---

## Part 2: Android App

### 2.1 Build Configuration

In `android/app/src/main/java/com/yawmiyati/app/data/api/ApiClient.kt`, update `BASE_URL`:

```
https://api.wathb.example.com/api/v1/
```

Do not hardcode credentials. The URL is the only environment-specific value needed for the production APK.

### 2.2 Build Debug APK (pilot sideload)

```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### 2.3 Install on Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or send the APK directly to pilot testers (WhatsApp / email is fine for internal pilot).

### 2.4 App Tunnel (during local dev only)

If the backend is running locally on port 8000:
```bash
adb reverse tcp:8000 tcp:8000
```
This is not needed when using a real server with HTTPS.

---

## Part 3: Pilot User Setup

### 3.1 User Roles

| Role | How to create |
|------|---------------|
| Worker | Register via the app (role = عامل) |
| Business | Register via the app (role = منشأة) |
| Admin | `createsuperuser` → set `role = ADMIN` in Django admin |

### 3.2 Business Verification

Admin must verify each business before it can post jobs:
1. Log in to Django admin at `https://api.wathb.example.com/admin/`
2. Go to **Businesses → Business profiles**
3. Set `is_verified = True` for approved businesses

### 3.3 First Job Flow

1. Admin verifies a business
2. Business posts a job (DRAFT → publish → PUBLISHED)
3. Worker applies
4. Business accepts → assignment ACTIVE
5. Business declares payment → worker acknowledges (cash) or admin verifies (bank transfer)
6. Obligation transitions to VERIFIED_PAID

---

## Part 4: Monitoring

- Check `gunicorn` error logs: `journalctl -u wathb-gunicorn -f`
- Database connections: `SELECT count(*) FROM pg_stat_activity WHERE datname = 'wathb_prod';`
- Disk space: `df -h` — ensure MEDIA_ROOT has headroom for payment evidence uploads

---

## Part 5: Shutdown Procedure

```bash
sudo systemctl stop wathb-gunicorn
# Run a final backup before any maintenance
/opt/wathb/scripts/backup.sh
```
