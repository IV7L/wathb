# Wathb

A daily-work marketplace connecting workers with businesses in Gaza, Palestine.

## What It Does

Workers find daily jobs, review conditions and wages before accepting, and track whether they have been paid. Businesses post jobs, select workers, confirm attendance, and document payment. Authorized funders may finance approved work. Administrators supervise, verify accounts, and resolve disputes.

## Stack

| Layer | Technology |
|-------|-----------|
| Android App | Kotlin, Jetpack Compose, Material 3 |
| Backend API | Python 3.12, Django 5, Django REST Framework |
| Database | PostgreSQL 14 |
| Auth | JWT (djangorestframework-simplejwt) |
| API Docs | drf-spectacular (OpenAPI 3) |

## Repository Layout

```
yawmiyati/
├── backend/          Django API server
├── android/          Android application (Milestone 6+)
└── docs/             Architecture, requirements, runbooks
```

## Local Development

### Backend

```bash
# Prerequisites: Python 3.12 (pyenv), PostgreSQL 14 running

cd backend
pyenv local 3.12.13
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

createdb yawmiyati_dev
cp .env.example .env
# Edit .env with local values

python manage.py migrate
python manage.py createsuperuser
python manage.py runserver 0.0.0.0:8000
```

### Android

See `docs/ENVIRONMENT_AUDIT.md` for SDK setup prerequisites. Android Studio required.

## Running Tests

```bash
cd backend
source .venv/bin/activate
python manage.py test
```

## Documentation

| Document | Purpose |
|----------|---------|
| [ENVIRONMENT_AUDIT.md](docs/ENVIRONMENT_AUDIT.md) | Dev environment state and setup |
| [PRODUCT_REQUIREMENTS.md](docs/PRODUCT_REQUIREMENTS.md) | Full product specification |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design decisions |
| [DATA_MODEL.md](docs/DATA_MODEL.md) | Database models and relationships |
| [API.md](docs/API.md) | API conventions and endpoint index |
| [SECURITY.md](docs/SECURITY.md) | Security design |
| [TEST_PLAN.md](docs/TEST_PLAN.md) | Test strategy and coverage targets |
| [IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) | Milestone breakdown |

## Status

Milestone 0 (environment audit) — complete
Milestone 1 (documentation) — complete
Milestone 2 (Django foundation) — in progress

## License

Private — all rights reserved. Not open for public use during pilot phase.
