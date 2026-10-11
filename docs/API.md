# API Design — Yawmiyati

## Base URL

```
Development (Mac host):  http://localhost:8000/api/v1/
Android emulator:        http://10.0.2.2:8000/api/v1/
```

## Conventions

### Authentication

All endpoints except `/api/v1/auth/register/` and `/api/v1/auth/login/` require:

```
Authorization: Bearer <access_token>
```

### Response Envelope

Success responses return the resource directly or as a paginated list:

```json
{
  "count": 42,
  "next": "http://...",
  "previous": null,
  "results": [...]
}
```

Error responses:

```json
{
  "error": "validation_error",
  "detail": "Human-readable message",
  "fields": {
    "wage_amount": ["This field is required."]
  }
}
```

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 204 | Deleted / No content |
| 400 | Validation error or bad request |
| 401 | Not authenticated or token expired |
| 403 | Authenticated but not authorized |
| 404 | Not found (or ownership denied) |
| 409 | Conflict (e.g., duplicate application) |
| 429 | Rate limited |
| 500 | Server error (details not exposed) |

### Pagination

Default page size: 20. Max: 100. Query params: `?page=2&page_size=50`.

### Filtering and Ordering

Supported via query parameters where documented. Example:
```
GET /api/v1/jobs/?status=PUBLISHED&city=khan_younis&ordering=-work_date
```

## Endpoint Index

### Auth

```
POST /api/v1/auth/register/
POST /api/v1/auth/login/
POST /api/v1/auth/token/refresh/
POST /api/v1/auth/logout/
GET  /api/v1/auth/me/
PATCH /api/v1/auth/me/
```

### Workers

```
GET   /api/v1/workers/profile/
PATCH /api/v1/workers/profile/
```

### Businesses

```
GET   /api/v1/businesses/profile/
PATCH /api/v1/businesses/profile/
GET   /api/v1/admin/businesses/                    (admin)
PATCH /api/v1/admin/businesses/{id}/verify/        (admin)
PATCH /api/v1/admin/businesses/{id}/suspend/       (admin)
```

### Jobs

```
GET    /api/v1/jobs/                               (worker: published; business: own)
POST   /api/v1/jobs/                               (business)
GET    /api/v1/jobs/{id}/
PATCH  /api/v1/jobs/{id}/                          (business, own)
DELETE /api/v1/jobs/{id}/                          (business, own, draft only)
POST   /api/v1/jobs/{id}/publish/                  (business)
POST   /api/v1/jobs/{id}/cancel/                   (business or admin)

GET    /api/v1/jobs/{id}/applications/             (business: own job)
POST   /api/v1/jobs/{id}/apply/                    (worker)
PATCH  /api/v1/jobs/{id}/applications/{app_id}/    (business: accept/reject offer)
DELETE /api/v1/jobs/{id}/applications/{app_id}/    (worker: withdraw)
```

### Assignments

```
GET   /api/v1/assignments/                         (worker: own; business: own jobs)
GET   /api/v1/assignments/{id}/
POST  /api/v1/assignments/{id}/accept/             (worker)
POST  /api/v1/assignments/{id}/decline/            (worker)
POST  /api/v1/assignments/{id}/cancel/             (business or admin)
```

### Attendance

```
POST  /api/v1/assignments/{id}/attendance/         (worker or business)
GET   /api/v1/assignments/{id}/attendance/
PATCH /api/v1/assignments/{id}/attendance/{rec_id}/confirm/    (other party)
PATCH /api/v1/assignments/{id}/attendance/{rec_id}/dispute/    (other party)
```

### Completion

```
POST  /api/v1/assignments/{id}/completion/         (worker or business)
PATCH /api/v1/assignments/{id}/completion/confirm/ (other party)
PATCH /api/v1/assignments/{id}/completion/dispute/ (other party)
```

### Wages

```
GET  /api/v1/wages/                                (worker: own; business: own jobs)
GET  /api/v1/wages/{id}/
POST /api/v1/wages/{id}/declare-payment/           (business)
POST /api/v1/wages/{id}/acknowledge/               (worker)
POST /api/v1/wages/{id}/dispute-payment/           (worker)
```

### Payment Evidence

```
POST /api/v1/wages/{id}/declarations/{dec_id}/evidence/    (business)
GET  /api/v1/wages/{id}/declarations/{dec_id}/evidence/{ev_id}/file/  (authenticated + authorized)
```

### Disputes

```
GET  /api/v1/disputes/                             (own disputes; admin: all)
GET  /api/v1/disputes/{id}/
POST /api/v1/disputes/                             (worker or business)
GET  /api/v1/disputes/{id}/events/
POST /api/v1/admin/disputes/{id}/assign/           (admin)
POST /api/v1/admin/disputes/{id}/resolve/          (admin)
```

### Funding

```
GET  /api/v1/funding/proposals/                    (funder: own; admin: all)
POST /api/v1/funding/proposals/
GET  /api/v1/funding/proposals/{id}/
GET  /api/v1/funding/proposals/{id}/budget/
POST /api/v1/admin/funding/proposals/{id}/approve/ (admin)
POST /api/v1/admin/funding/proposals/{id}/reject/  (admin)
```

### Notifications

```
GET   /api/v1/notifications/
PATCH /api/v1/notifications/{id}/read/
POST  /api/v1/notifications/read-all/
```

### Admin

```
GET  /api/v1/admin/accounts/
PATCH /api/v1/admin/accounts/{id}/verify/
PATCH /api/v1/admin/accounts/{id}/suspend/
GET  /api/v1/admin/audit-log/
GET  /api/v1/admin/reports/pilot-summary/
GET  /api/v1/admin/reports/pilot-summary/export/   (CSV)
```

### System

```
GET /api/v1/health/
GET /api/v1/schema/                                (OpenAPI YAML)
GET /api/v1/schema/swagger-ui/                     (Swagger UI, dev only)
```

## OpenAPI Documentation

Available at `/api/v1/schema/swagger-ui/` in development. Generated by `drf-spectacular`.
