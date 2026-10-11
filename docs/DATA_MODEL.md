# Data Model — Yawmiyati

Money amounts are stored as integers in minor units. ILS amounts are in agorot (1 ILS = 100 agorot). USD in cents. Currency code stored alongside every monetary field.

## accounts app

### User (custom AbstractBaseUser)

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| email | EmailField unique | login identifier |
| phone | CharField | optional |
| role | CharField | WORKER / BUSINESS / FUNDER / ADMIN |
| is_verified | BooleanField | set by admin |
| is_active | BooleanField | |
| is_staff | BooleanField | Django admin access |
| date_joined | DateTimeField | |
| last_login | DateTimeField | |

### WorkerProfile (OneToOne → User where role=WORKER)

| Field | Type |
|-------|------|
| full_name | CharField |
| location_city | CharField |
| location_region | CharField |
| skills | TextField |
| experience_summary | TextField |
| national_id_hash | CharField (nullable) — SHA-256 of ID, not the ID itself |
| is_available | BooleanField |

### BusinessProfile (OneToOne → User where role=BUSINESS)

| Field | Type |
|-------|------|
| legal_name | CharField |
| trade_name | CharField |
| registration_number | CharField nullable |
| location_address | CharField |
| location_city | CharField |
| contact_name | CharField |
| contact_phone | CharField |
| verification_status | CharField — PENDING / APPROVED / REJECTED / SUSPENDED |
| verified_by | FK → User (admin) nullable |
| verified_at | DateTimeField nullable |
| suspension_reason | TextField nullable |

## jobs app

### JobListing

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| business | FK → BusinessProfile | |
| title | CharField | |
| description | TextField | |
| location_address | CharField | |
| location_city | CharField | |
| work_date | DateField | |
| start_time | TimeField | |
| end_time | TimeField | |
| num_workers_needed | PositiveIntegerField | |
| required_skills | TextField | |
| wage_amount | PositiveBigIntegerField | minor units |
| wage_currency | CharField | ILS / USD |
| wage_basis | CharField | PER_DAY / PER_TASK / PER_HOUR |
| payment_method | CharField | CASH / BANK_TRANSFER / OTHER |
| payment_deadline_days | PositiveIntegerField | days after completion |
| attendance_requirements | TextField | |
| cancellation_policy | TextField | |
| additional_conditions | TextField | |
| status | CharField | DRAFT / PENDING_REVIEW / PUBLISHED / FILLED / IN_PROGRESS / COMPLETED / CANCELLED |
| is_funded | BooleanField | |
| funding_allocation | FK → FundingAllocation nullable | |
| published_at | DateTimeField nullable | |
| version | PositiveIntegerField | incremented on material change |
| created_at | DateTimeField | |
| updated_at | DateTimeField | |

### JobApplication

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| job | FK → JobListing | |
| worker | FK → User (role=WORKER) | |
| status | CharField | PENDING / OFFERED / ACCEPTED / REJECTED / WITHDRAWN / EXPIRED |
| cover_note | TextField nullable | |
| applied_at | DateTimeField | |
| reviewed_at | DateTimeField nullable | |

Unique constraint: (job, worker)

## assignments app

### Assignment

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| job | FK → JobListing | |
| worker | FK → User | |
| application | OneToOne → JobApplication | |
| terms_snapshot | JSONField | immutable copy at acceptance time |
| status | CharField | OFFERED / ACCEPTED / DECLINED / ACTIVE / COMPLETED / CANCELLED |
| offered_at | DateTimeField | |
| accepted_at | DateTimeField nullable | |
| cancelled_at | DateTimeField nullable | |
| cancellation_reason | TextField nullable | |

Unique constraint: (job, worker)

### AttendanceRecord

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| assignment | FK → Assignment | |
| submitted_by | FK → User | |
| submitted_at | DateTimeField | |
| work_date | DateField | |
| check_in_time | TimeField nullable | |
| check_out_time | TimeField nullable | |
| notes | TextField nullable | |
| status | CharField | SUBMITTED / CONFIRMED / DISPUTED / CANCELLED |
| confirmed_by | FK → User nullable | |
| confirmed_at | DateTimeField nullable | |

### CompletionRecord

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| assignment | OneToOne → Assignment | prevents duplicate completion |
| submitted_by | FK → User | |
| submitted_at | DateTimeField | |
| hours_worked | DecimalField nullable | |
| notes | TextField nullable | |
| status | CharField | SUBMITTED / CONFIRMED / DISPUTED |
| confirmed_by | FK → User nullable | |
| confirmed_at | DateTimeField nullable | |

## wages app

### WageObligation

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| assignment | OneToOne → Assignment | prevents duplicate wage creation |
| amount | PositiveBigIntegerField | minor units |
| currency | CharField | |
| due_date | DateField | |
| status | CharField | PENDING / DUE / DECLARED / AWAITING_ACK / UNDER_REVIEW / DISPUTED / VERIFIED_PAID / REJECTED / CANCELLED |
| created_at | DateTimeField | |
| updated_at | DateTimeField | |

### PaymentDeclaration

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| wage_obligation | FK → WageObligation | |
| declared_amount | PositiveBigIntegerField | minor units |
| currency | CharField | |
| payment_date | DateField | |
| payment_method | CharField | |
| transaction_reference | CharField nullable | |
| declared_by | FK → User | business user |
| declared_at | DateTimeField | |
| status | CharField | SUBMITTED / ACKNOWLEDGED / DISPUTED / VERIFIED / REJECTED |
| reviewed_by | FK → User nullable | admin |
| reviewed_at | DateTimeField nullable | |
| review_notes | TextField nullable | |

### PaymentEvidence

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| declaration | FK → PaymentDeclaration | |
| file | FileField | private storage |
| original_filename | CharField | |
| content_type | CharField | server-validated |
| file_size | PositiveBigIntegerField | bytes |
| uploaded_by | FK → User | |
| uploaded_at | DateTimeField | |

## disputes app

### Dispute

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| category | CharField | WORKER_NO_SHOW / ATTENDANCE_DISPUTED / WORK_INCOMPLETE / WAGE_DISPUTED / PAYMENT_NOT_RECEIVED / EVIDENCE_INSUFFICIENT / CONDITIONS_CHANGED / CANCELLATION / OTHER |
| assignment | FK → Assignment | |
| wage_obligation | FK → WageObligation nullable | |
| raised_by | FK → User | |
| description | TextField | |
| status | CharField | OPEN / IN_REVIEW / RESOLVED / CLOSED |
| assigned_to | FK → User nullable | admin |
| created_at | DateTimeField | |
| resolved_at | DateTimeField nullable | |
| resolution_notes | TextField nullable | |

### DisputeEvent

| Field | Type |
|-------|------|
| id | UUID PK |
| dispute | FK → Dispute |
| actor | FK → User |
| action | CharField |
| notes | TextField nullable |
| timestamp | DateTimeField |

## funding app

### FundingProposal

| Field | Type |
|-------|------|
| id | UUID PK |
| funder | FK → User (role=FUNDER) |
| title | CharField |
| description | TextField |
| total_amount | PositiveBigIntegerField |
| currency | CharField |
| status | CharField — PROPOSED / PLEDGED / APPROVED / ACTIVE / COMPLETED / CANCELLED |
| approved_by | FK → User nullable |
| approved_at | DateTimeField nullable |
| created_at | DateTimeField |

### FundingBudget

| Field | Type | Notes |
|-------|------|-------|
| id | UUID PK | |
| proposal | OneToOne → FundingProposal | |
| total_amount | PositiveBigIntegerField | approved amount |
| currency | CharField | |
| reserved_amount | PositiveBigIntegerField | default 0 |
| allocated_amount | PositiveBigIntegerField | default 0 |
| spent_amount | PositiveBigIntegerField | default 0 |

Invariant: `reserved_amount + allocated_amount + spent_amount <= total_amount`

### FundingAllocation

| Field | Type |
|-------|------|
| id | UUID PK |
| budget | FK → FundingBudget |
| job_listing | OneToOne → JobListing |
| amount | PositiveBigIntegerField |
| currency | CharField |
| status | CharField — RESERVED / ALLOCATED / RELEASED / SPENT |
| created_at | DateTimeField |
| updated_at | DateTimeField |

## audit app

### AuditEvent

| Field | Type |
|-------|------|
| id | UUID PK |
| actor | FK → User nullable (system events allowed) |
| action | CharField |
| model_name | CharField |
| object_id | CharField |
| details | JSONField |
| timestamp | DateTimeField auto |
| ip_address | GenericIPAddressField nullable |

AuditEvent records are never deleted or modified.

## notifications app

### Notification

| Field | Type |
|-------|------|
| id | UUID PK |
| recipient | FK → User |
| notification_type | CharField |
| title | CharField |
| body | TextField |
| related_model | CharField nullable |
| related_id | CharField nullable |
| is_read | BooleanField default False |
| created_at | DateTimeField |
