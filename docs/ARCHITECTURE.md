# Architecture — Yawmiyati

## System Overview

```
┌────────────────────────────────────┐
│  Android App (Kotlin/Compose)      │
│  - Arabic RTL, Material 3          │
│  - Role-aware navigation           │
│  - Retrofit + OkHttp               │
└──────────────┬─────────────────────┘
               │ HTTPS / HTTP (dev: 10.0.2.2:8000)
               │ JSON REST API
┌──────────────▼─────────────────────┐
│  Django + DRF (Python 3.12)        │
│  - JWT authentication              │
│  - Role + object-level permissions │
│  - Service-layer business logic    │
│  - OpenAPI docs (drf-spectacular)  │
└──────────────┬─────────────────────┘
               │ psycopg2
┌──────────────▼─────────────────────┐
│  PostgreSQL 14                     │
│  - ACID transactions               │
│  - Row-level locking for budgets   │
└────────────────────────────────────┘
```

## Backend Structure

```
backend/
├── yawmiyati/          Django project config
│   └── settings/       base / development / test / production
├── accounts/           Custom user model, roles, profiles
├── businesses/         Business registration and verification
├── jobs/               Listings, applications, offers, state machines
├── assignments/        Accepted terms snapshots, attendance, completion
├── wages/              Wage obligations, payment declarations, evidence
├── disputes/           Dispute lifecycle and resolution
├── funding/            Funding proposals, budgets, allocations
├── notifications/      In-app notification model
├── audit/              Immutable audit event log
└── reporting/          Aggregated reports and CSV exports
```

## Authentication

JWT access + refresh token pair.
- Access token: 15-minute expiry.
- Refresh token: 7-day expiry, rotated on use.
- Stored by client in secure storage (Android EncryptedSharedPreferences).
- All sensitive endpoints require a valid access token.
- Session-expiry handled gracefully on the Android side.

## Authorization Model

Four roles: `WORKER`, `BUSINESS`, `FUNDER`, `ADMIN`.

Every API endpoint enforces:
1. Authentication (valid token required).
2. Role permission (DRF `IsAuthenticated` + custom role permissions).
3. Object ownership (business can only modify its own jobs; worker can only view their own assignments).

Authorization is never delegated to the client. Button hiding is cosmetic only.

## State Machines

Separate, explicit state machines for:
- `JobListing.status`
- `JobApplication.status`
- `Assignment.status`
- `AttendanceRecord.status`
- `CompletionRecord.status`
- `WageObligation.status`
- `PaymentDeclaration.status`
- `Dispute.status`
- `FundingProposal.status`
- `FundingAllocation.status`

Transitions are validated server-side. Invalid transitions return 400 with a clear error.

## Terms Snapshot

When a worker accepts an assignment, the system creates an immutable JSON snapshot of the job's material terms at that moment. If the job listing is later edited, the snapshot is unaffected. The snapshot is stored on `Assignment.terms_snapshot` and is part of the wage obligation record.

## Financial Integrity

- Monetary amounts stored as integers in minor currency units (e.g., agorot for ILS, cents for USD).
- Currency stored explicitly alongside every monetary field.
- No floating-point arithmetic for money.
- Budget allocation uses `SELECT ... FOR UPDATE` to prevent concurrent overspending.
- Wage obligations are created exactly once per completed assignment (unique constraint + service-layer guard).
- Payment declared ≠ payment verified. Status field enforces this distinction.

## File Storage

Uploaded evidence files:
- Stored outside the web-server public directory (private media).
- Access only via authenticated, authorized API endpoint.
- File type validated server-side (not trusted from client).
- File size limited.
- Storage backend configurable (local filesystem in dev, object storage in production).

## Development vs. Production

| Concern | Development | Production |
|---------|------------|------------|
| Database | Local PostgreSQL 14 | PostgreSQL (managed or self-hosted) |
| File storage | Local filesystem | Private object storage (e.g., S3-compatible) |
| Secret management | `.env` file | Environment variables / secrets manager |
| Debug | `DEBUG=True` | `DEBUG=False` |
| CORS | Permissive (emulator) | Restricted |
| HTTPS | Not required | Required |
| Allowed hosts | `localhost`, `10.0.2.2` | Production domain only |

## Connectivity Design

The backend is authoritative. All financial and workflow decisions are server-side.

The Android app:
- Displays clear error states for network failures.
- Shows retry options.
- Does not report success until the server confirms.
- Handles 401 (expired token) by redirecting to login.
- Labels previously fetched data as potentially stale when offline.

## Key Constraints

- No external payment gateway in MVP.
- No external identity verification service.
- No AI/ML service calls in the data path.
- No GPS tracking or biometric collection.
- No cryptocurrency or blockchain.
