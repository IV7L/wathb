# Test Plan — Yawmiyati

## Backend Tests (Django)

All backend tests use Django's `TestCase` and DRF's `APITestCase`. Database is a real PostgreSQL test database.

### accounts

- [ ] Register worker with valid data
- [ ] Register business with valid data
- [ ] Duplicate email rejected
- [ ] Login returns JWT pair
- [ ] Invalid password rejected
- [ ] Expired token rejected on protected endpoint
- [ ] Worker cannot access business-only endpoint
- [ ] Business cannot access worker-only endpoint
- [ ] Funder cannot access admin endpoint
- [ ] Admin can access all management endpoints

### businesses

- [ ] New business starts as PENDING verification
- [ ] Admin can approve business
- [ ] Admin can reject business with reason
- [ ] Admin can suspend business with reason
- [ ] Business cannot approve itself
- [ ] Approved business can publish jobs; pending business cannot

### jobs

- [ ] Business can create draft job with all required fields
- [ ] Missing required field returns 400 with field errors
- [ ] Business can publish a valid draft
- [ ] Published job is visible to workers
- [ ] Unverified business cannot publish
- [ ] Worker can apply to published job
- [ ] Duplicate application returns 409
- [ ] Business can accept an application (→ OFFERED)
- [ ] Worker can accept the offer (→ ACCEPTED, Assignment created)
- [ ] Worker accepts → terms snapshot is saved on Assignment
- [ ] Snapshot unchanged when job listing is later edited
- [ ] Business can reject an application
- [ ] Worker can withdraw application before offer
- [ ] Filled job not visible to new applicants

### assignments

- [ ] Accepted assignment moves to ACTIVE status
- [ ] Same worker cannot have two ACTIVE assignments for the same job
- [ ] Worker can submit attendance
- [ ] Business can confirm attendance
- [ ] Business can dispute attendance
- [ ] Worker can submit completion
- [ ] Business can confirm completion
- [ ] CompletionRecord created at most once per assignment (OneToOne enforced)
- [ ] Confirming completion with disputed attendance routes to dispute
- [ ] WageObligation created exactly once when completion confirmed
- [ ] WageObligation amount matches terms_snapshot wage, not any later listing change

### wages and payments

- [ ] WageObligation status starts as PENDING
- [ ] Business declares payment → status moves to DECLARED
- [ ] Worker acknowledges → VERIFIED_PAID
- [ ] Worker disputes → DISPUTED, Dispute created
- [ ] Admin reviews evidence and can mark VERIFIED or REJECTED
- [ ] Declaring payment does not auto-set VERIFIED_PAID
- [ ] Evidence file upload accepted for jpeg/png/pdf under 5MB
- [ ] Oversized file rejected
- [ ] Disallowed content type rejected
- [ ] Evidence file only accessible to authorized parties
- [ ] Unauthorized user cannot access evidence file

### disputes

- [ ] Worker can open dispute with category and description
- [ ] Business can open dispute
- [ ] Dispute starts as OPEN
- [ ] Admin can assign dispute to themselves
- [ ] Admin can resolve dispute with notes
- [ ] Resolution recorded in DisputeEvent
- [ ] Disputed wage is flagged, not erased
- [ ] Resolved dispute does not auto-verify the payment

### funding

- [ ] Funder creates proposal → PROPOSED
- [ ] Admin approves proposal → APPROVED, FundingBudget created
- [ ] Admin rejects proposal with reason
- [ ] Funded job requires approved allocation before PUBLISHED
- [ ] Allocation reserves budget amount (reserved_amount increases)
- [ ] Cannot allocate more than available budget (invariant)
- [ ] Concurrent allocation requests do not overspend budget
- [ ] Releasing allocation returns amount to available
- [ ] Completed funded work moves allocation to SPENT

### audit

- [ ] Admin approval creates AuditEvent
- [ ] Payment verification creates AuditEvent
- [ ] Dispute resolution creates AuditEvent
- [ ] AuditEvent cannot be deleted via API
- [ ] AuditEvent cannot be modified

### reporting

- [ ] Pilot summary counts verified businesses correctly
- [ ] Pilot summary counts completed assignments
- [ ] Pilot summary counts wage obligations created
- [ ] Pilot summary counts payments verified paid
- [ ] Completed worker-day ≠ paid worker-day (counted separately)
- [ ] CSV export returns valid CSV

## Integration / End-to-End Tests

### Scenario A: Full happy path

1. Admin verifies business
2. Business publishes job
3. Worker finds and applies
4. Business offers slot to worker
5. Worker accepts (terms snapshot saved)
6. Business changes wage after acceptance — snapshot unchanged
7. Attendance submitted and confirmed
8. Completion submitted and confirmed
9. WageObligation created (once)
10. Business declares payment with evidence
11. Worker acknowledges receipt
12. Status: VERIFIED_PAID

### Scenario B: Disputed payment

Same as A through step 9, then:

10. Business declares payment
11. Worker disputes (no payment received)
12. Dispute created, WageObligation flagged as DISPUTED
13. Admin reviews evidence, resolves dispute
14. Final state recorded

### Scenario C: Funding — insufficient budget

1. Funder proposes budget of 4000 agorot
2. Admin approves
3. Job A requires 3000 agorot allocation — succeeds
4. Job B requires 2000 agorot allocation — fails (only 1000 remaining)
5. Job B rejected correctly

### Scenario D: Concurrent allocation overspend prevention

1. Budget of 5000 agorot
2. Two allocation requests of 3000 agorot each sent concurrently
3. Exactly one succeeds, one fails
4. Final reserved_amount ≤ 5000

## Android Tests

- [ ] Project assembles (`./gradlew assembleDebug`)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] ViewModel renders correct state from API response
- [ ] ViewModel renders error state on API failure
- [ ] ViewModel renders loading state
- [ ] Job list shows empty state when no jobs available
- [ ] Job detail screen renders Arabic text RTL
- [ ] Login screen navigates to correct home on success
- [ ] Unauthorized response triggers logout/re-login
- [ ] Role-based navigation: worker does not see business screens

## Test Infrastructure

- Backend: `python manage.py test` against real PostgreSQL test database
- Android: `./gradlew test` (unit) and `./gradlew connectedAndroidTest` (emulator, Milestone 8)
- Integration: Django management command or pytest script
- All tests must be repeatable without network access to external services
