# Implementation Plan — Yawmiyati

## Milestone Status

| Milestone | Status | Notes |
|-----------|--------|-------|
| 0 — Environment Audit | Complete | See ENVIRONMENT_AUDIT.md |
| 1 — Documentation | Complete | This document |
| 2 — Django Foundation | In Progress | |
| 3 — Jobs & Assignments | Pending | |
| 4 — Attendance, Wages, Disputes | Pending | |
| 5 — Funding & Reporting | Pending | |
| 6 — Android Foundation | Pending | Requires SDK setup |
| 7 — Android Workflows | Pending | |
| 8 — End-to-End Verification | Pending | |
| 9 — Pilot Readiness | Pending | |

## Milestone 2 — Django Foundation

### Scope

- Django 5.1 project at `backend/`
- Custom user model (UUID PK, email login, role field)
- `accounts` app: User, WorkerProfile, BusinessProfile
- `audit` app: AuditEvent model
- `notifications` app: Notification model
- Django REST Framework configured
- JWT auth (`djangorestframework-simplejwt`)
- OpenAPI (`drf-spectacular`)
- Settings split: base / development / test
- PostgreSQL configured via env vars
- Initial migrations
- Health endpoint
- Register, login, refresh, me endpoints
- Role-based permission classes
- Tests: registration, login, role isolation

### Acceptance Criteria

- `python manage.py migrate` runs without errors
- `python manage.py test accounts` passes all tests
- Health endpoint returns 200 with JSON
- Login returns JWT pair
- Protected endpoint rejects missing/invalid token

## Milestone 3 — Jobs & Assignments

### Scope

- `businesses` app: BusinessProfile, verification workflow
- `jobs` app: JobListing, JobApplication state machines
- `assignments` app: Assignment, terms snapshot
- Business verification workflow (admin)
- Job publication validation
- Worker application and offer acceptance
- Terms snapshot saved on acceptance
- Snapshot preserved when listing changes
- AuditEvents for key transitions
- Tests: permissions, lifecycle, snapshot integrity

## Milestone 4 — Attendance, Wages, Disputes

### Scope

- Attendance and completion workflows
- Wage obligation creation (exactly once, from snapshot)
- Payment declaration and evidence upload
- File security: private storage, validated content types
- Worker acknowledgement and payment dispute
- Admin payment review
- Dispute lifecycle
- Financial invariants tested
- Full end-to-end scenarios A and B

## Milestone 5 — Funding & Reporting

### Scope

- FundingProposal, FundingBudget, FundingAllocation
- Budget invariant: concurrent allocation protection
- Funded job publication gate
- Pilot summary report endpoint
- CSV export
- End-to-end scenario C and D

## Milestone 6 — Android Foundation

### Prerequisites

- Android SDK Platform 35 installed via SDK Manager
- AVD created and running
- `ANDROID_HOME` set

### Scope

- Kotlin/Compose project at `android/`
- Material 3 theme, Arabic RTL locale
- Navigation graph (role-aware)
- Retrofit API client, base URL from `BuildConfig`
- Login / registration screens
- JWT token storage (`EncryptedSharedPreferences`)
- 401 → re-login flow
- Loading, error, empty states
- Job list and detail screens (read-only)

## Milestone 7 — Android Workflows

- Worker: apply, accept offer, attendance, completion, wages, disputes
- Business: create/publish job, manage applications, confirm attendance, declare payment
- Admin: account verification, dispute review (via Django admin or basic screens)

## Milestone 8 — End-to-End Verification

- Backend full test suite
- Android build and unit tests
- Integration scenarios A–D verified
- Manual emulator smoke-test against local Django server

## Milestone 9 — Pilot Readiness

- PILOT_RUNBOOK.md
- DEPLOYMENT.md
- ADMIN_GUIDE.md
- KNOWN_LIMITATIONS.md
- Admin user creation script
- Database backup instructions

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Python version | 3.12.13 (pyenv) | LTS-track, stable Django 5 support |
| Django version | 5.1 | Current stable |
| Auth | djangorestframework-simplejwt | Mature, well-documented JWT for DRF |
| OpenAPI | drf-spectacular | Best DRF OpenAPI 3 support |
| Database | PostgreSQL 14 (local, already running) | Production-compatible, no Docker needed |
| Money | Integer minor units + explicit currency | Avoids float precision errors |
| Android min SDK | 26 (Android 8) | 94%+ device coverage; below this Jetpack Compose features are unavailable |
| Android target SDK | 35 (Android 15) | Latest stable |
| File content validation | python-magic or imghdr + struct | Cannot trust client MIME type |
