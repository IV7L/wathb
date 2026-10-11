# Product Requirements — Yawmiyati

## Purpose

A daily-work marketplace for Gaza. Workers find legitimate daily jobs, understand their conditions and wages before accepting, and track whether they have been paid. Businesses post jobs, select workers, and document payment. Authorized funders may support approved work.

## Guiding Principle

A worker must know what they are agreeing to, what they must do, how much they should earn, and whether their wage has been paid.

## Users

### Worker

- Register and maintain a profile (name, contact, location, skills).
- Browse published jobs; filter by location, category, wage, date.
- Review complete job conditions before applying.
- Apply; view application status.
- Accept an offered job under explicit, documented terms.
- Submit attendance and confirm work completion.
- View earned wages, payment status, and payment evidence.
- Raise and track disputes.
- View work history.
- Never charged a fee to access or claim wages.

### Business

- Register and submit establishment information for verification.
- Create and manage job listings with all required terms.
- Review, accept, or reject applicants.
- Record attendance and submit completion confirmation.
- Declare payment with supporting evidence.
- Respond to disputes.
- May not approve its own verification or resolve its own disputes.

### Funder

- View funding opportunities.
- Review proposed budgets and outcomes.
- Submit funding commitments for authorized review.
- Track approved budgets, allocations, utilization, and verified outcomes.
- May not manipulate attendance, wages, eligibility, payment evidence, or disputes.

### Administrator

- Review and verify worker and business accounts.
- Suspend or reactivate accounts (with reason).
- Moderate job listings.
- Review and resolve disputes.
- Verify or reject payment evidence.
- Approve funding proposals and allocations.
- Review audit logs and reports.
- Permissions are granular; not all admin actions available to all staff.

## Job Lifecycle

1. Business creates listing.
2. Backend validates required fields and business eligibility.
3. Listing published when conditions satisfied.
4. Workers browse and apply.
5. Business selects worker.
6. Worker explicitly accepts the offer and its conditions.
7. System records immutable snapshot of accepted terms.
8. Attendance submitted and confirmed.
9. Work completion submitted and confirmed.
10. Wage obligation recorded.
11. Business declares payment with evidence.
12. Worker acknowledges receipt or raises dispute.
13. Authorized reviewers investigate unresolved cases.
14. Record reaches final state; complete history retained.

## Required Job Terms

- Title and description
- Business identity and verification status
- Work location
- Work date and expected hours
- Number of required workers
- Required skills
- Wage amount, currency, and basis (per day / per task / per hour)
- Payment method
- Expected payment deadline
- Attendance and completion requirements
- Cancellation conditions
- Additional material conditions

## Wage Rules

- Wage amount from the accepted-terms snapshot — not the current listing.
- Wage obligation created exactly once per completed assignment.
- Payment declared ≠ payment verified.
- Disputed wages preserved, not erased.
- 40 NIS/day is a pilot planning assumption, not a hardcoded rule.

## Payment Workflow

Manual only in MVP. No payment gateway.

States: Not yet due → Due → Payment declared → Awaiting worker acknowledgement → Under review → Verified paid (or Disputed / Rejected).

## Funding Rules

- Funded work must not begin until funding approved and budget reserved.
- Budget invariants enforced at database level.
- `SELECT ... FOR UPDATE` prevents concurrent overspend.
- Distinct states: Proposed / Pledged / Approved / Active / Completed / Cancelled.
- Pledge ≠ received money. Allocation ≠ spent money.

## Privacy

- Collect only what is required for the product's functions.
- No continuous GPS, biometrics, or unnecessary identity documents.
- Private profile data, receipts, evidence, and documents restricted to authorized parties.
- Synthetic data in development and tests.

## Connectivity

- Clear error and retry states.
- No duplicate submissions on retry (server idempotency).
- Server authoritative for all financial and workflow decisions.
- Stale data labeled as such.

## Pilot Context

- Initial pilot: Khan Younis, Gaza — 30 days.
- Targets: 20 verified businesses, 50 verified workers, 100 paid worker-days.
- These are targets, not verified results.
- Not hardcoded in business logic — configurable reporting parameters.

## Out of Scope (MVP)

- Payment gateway or mobile money integration.
- SMS or push notification providers.
- Biometric verification.
- GPS attendance.
- AI-assisted matching or review.
- Multi-currency conversion.
- Web browser interface.
