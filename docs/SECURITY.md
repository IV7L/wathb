# Security Design — Yawmiyati

## Authentication

- Django's built-in password hashing (PBKDF2 with SHA256, salted).
- JWT issued by `djangorestframework-simplejwt`.
- Access tokens expire after 15 minutes.
- Refresh tokens expire after 7 days and rotate on use.
- Android stores tokens in `EncryptedSharedPreferences`.
- Expired-token responses (401) trigger re-login flow on Android.

## Authorization

- Every endpoint requires a valid access token (`IsAuthenticated`).
- Role-based permission classes (`IsWorker`, `IsBusiness`, `IsFunder`, `IsAdmin`).
- Object-level ownership checks in every relevant viewset.
- Authorization enforced server-side — client UI is cosmetic only.
- Business cannot modify another business's records.
- Worker cannot view another worker's private data.
- Funder sees only their own proposals and authorized summaries.

## Financial Field Protection

- Wage amounts, payment statuses, and funding balances are never set from client-supplied values.
- All monetary fields are computed or validated server-side.
- State transitions validated against allowed transitions enum.
- `SELECT ... FOR UPDATE` on `FundingBudget` rows during allocation.

## Input Validation

- All inputs validated by DRF serializers.
- Extra fields rejected (`extra_kwargs` or `read_only_fields` enforced).
- Numeric bounds checked for monetary amounts.
- Date/time fields range-validated where applicable.

## File Upload Security

- Allowed content types: `image/jpeg`, `image/png`, `application/pdf`.
- Maximum file size: 5 MB (configurable via `MAX_UPLOAD_SIZE_BYTES`).
- Content type validated by reading file magic bytes, not by trusting the HTTP header.
- Files stored in private media directory (not web-accessible).
- Served only via an authenticated API view that checks ownership.
- File paths are not returned as direct download URLs.
- No user-controlled filenames used on disk — UUID-based naming.

## Sensitive Data

- Passwords never logged.
- Tokens never logged.
- National ID numbers stored only as salted SHA-256 hash.
- Uploaded evidence files excluded from public static paths.
- `.env` not committed to Git.
- Test fixtures use synthetic data only.

## CORS

- Development: allows `http://localhost:*` and Android emulator origin.
- Production: restricted to the production domain. `CORS_ALLOWED_ORIGINS` from environment.

## Rate Limiting

- Login endpoint: `django-ratelimit` or DRF throttle, max 10 attempts per minute per IP.
- Token refresh: throttled similarly.
- Registration: throttled per IP.

## Admin and Audit

- Django admin interface restricted to `is_staff=True` users.
- All consequential actions (approval, rejection, payment verification, dispute resolution, account suspension) create an `AuditEvent` record.
- `AuditEvent` rows are write-once — never updated or deleted.

## Secrets Management

- All secrets via environment variables (`.env` locally, environment injection in production).
- `.env` excluded from Git via `.gitignore`.
- `.env.example` contains only placeholder values.
- `SECRET_KEY`, database credentials, and storage credentials never hardcoded.

## Transport Security

- Development: HTTP (local only).
- Production: HTTPS required. `SECURE_SSL_REDIRECT=True`, `HSTS` headers set.

## Known Limitations (MVP)

- No email verification on registration (can be added pre-pilot).
- Manual payment verification — no cryptographic proof of payment.
- No formal penetration test has been performed.
- Evidence file content is not scanned for malware.
- No two-factor authentication in MVP.
