WATHB WORK ORDER #2

PROTOCOL
1. First, save this entire message verbatim as docs/WORK_ORDER_2.md
   and commit it. Re-read the relevant part before each item.
2. Work strictly in order. One commit per item. After each item run
   both test suites (Django + Android JVM) and add tests for new
   behavior.
3. After each item, append to docs/WORK_ORDER_2_STATUS.md:
   item id, status (done / partial / skipped + reason), commit hash.
4. Never mark an item done if any sub-point was skipped. Write
   "partial" and list what is missing.
5. STOP and wait for my approval ONLY at points marked [STOP].
6. If context runs low: finish the current item, update the status
   file, stop with a summary.
7. All UI text in Arabic, RTL. Report to me in Arabic.
8. The server decides everything; the app only displays. Money
   stays integer minor units. Every new write goes to the audit log.
9. Push at the end of each part and confirm the live API
   (https://wathb-api2.onrender.com/api/v1/) reflects it with one
   curl.

PART 0 - FINISH PHASE 1 (verify each point; skip only if truly done)
0.1 Commit any uncommitted work. Show me the build.gradle.kts diff
    (no secrets). A clean git worktree of HEAD must build and pass.
0.2 Add termsSnapshot to AssignmentResponse. Show the agreed terms
    on the worker's assignment card for every status after
    acceptance, read from terms_snapshot, not the live job fields.
0.3 Hide the confirm button (attendance, completion) when the
    record was submitted by the current user's side. Show
    "بانتظار تأكيد الطرف الآخر" instead.
0.4 Authenticator tests with MockWebServer: 401 -> refresh -> retry
    succeeds; two parallel 401s -> exactly ONE refresh; refresh
    returns 401 -> tokens cleared, sessionExpired emitted once.
0.5 Is payment_method validated against fixed choices on the
    server? If not, fix. Tell me under what conditions an admin can
    set VERIFIED_PAID without the worker's acknowledgement.
0.6 Add backend/build.sh (pip install, collectstatic, migrate,
    ensure_admin from A3). Tell me the exact Build and Start
    commands to paste into Render.

PART A - ACCOUNTS AND IDENTITY
A1 Phone login.
   - Phone is the login identifier for ALL roles: required, unique,
     stored in one canonical form (E.164, +970). 0599123456,
     +970599123456, 00970599123456 and +972599123456 resolve to
     the same account. Accept only Palestinian mobiles (059, 056).
   - Email becomes optional for all roles.
   - Android: login and register use a phone field (numeric
     keyboard, digits left-to-right). Remove email from worker
     registration; optional for business. Replace every place that
     shows or passes an email as identity (e.g. route
     business_assignment/{id}/{email}) with name and phone.
   - No SMS, no OTP. Add a Django admin action to reset a user's
     password.
   - Rate limit login per phone as well as per IP.
   [STOP] Before coding A1, show me: the migration plan
   (USERNAME_FIELD change, existing rows without phone or with
   duplicates; the live DB holds only demo and test data, so
   wiping it is an option), the effect on createsuperuser and
   Django admin, and the full list of NEW env vars Part A to E
   need on Render.
A2 Registration accepts ONLY roles WORKER and BUSINESS. Anything
   else returns 400. Show current behavior first; add a test.
A3 Management command ensure_admin: creates or updates an ADMIN
   from env vars, safe to run repeatedly, supports more than one
   admin.
A4 National ID: replace SHA-256 with HMAC-SHA256 keyed by env var
   NATIONAL_ID_HMAC_KEY. Fail at startup in production if missing.
   Normalize before hashing. Tell me if any rows hold the old hash
   and whether the app collects the ID anywhere.
A5 Consent at registration: a short terms + privacy screen the
   user must accept. Store consent version and accepted_at. Write
   a clear Arabic draft and mark it as a draft for me to review.
A6 Worker verification: unverified workers CAN apply. Businesses
   see a موثّق / غير موثّق badge next to each applicant.

PART B - WORKER
B1 Profile screen: view and edit name, city/area, job categories
   (from C3), availability.
B2 "طلباتي": the worker's applications with status (معلّق / معروض /
   مرفوض / مسحوب) and a withdraw button. Add the backend endpoint
   if it does not exist.
B3 Business track record on the job card and detail, computed
   server-side from wage obligations, aggregates only:
   "دفعت X يوم · Y في الوقت" (on time = worker acknowledged within
   the payment deadline). No history: "منشأة جديدة".
B4 After acceptance: call button (local format) and WhatsApp
   button (wa.me with the +970 form) to the business.

PART C - BUSINESS
C1 Profile screen: view and edit business details.
C2 "انشر مرة ثانية": duplicate a past job into a new DRAFT with a
   new date.
C3 Job category: a fixed server-side list (تحميل وتنزيل، تنظيف،
   بناء وترميم، زراعة، نقل وتوصيل، مطاعم ومطابخ، محلات وبيع، أخرى),
   required on job creation, icon on the job card, filter chips on
   the worker's job list.
C4 Applicants list:
   - Show reliability only: days attended vs no-shows. Do NOT
     show total days worked or any ranking.
   - No history: a neutral "جديد" badge.
   - Default order: fewest confirmed work days in the current
     month first, then application time. The business still picks
     freely.
   - Call and WhatsApp buttons to the applicant.
C5 Cancel a PUBLISHED job, and cancel an ACCEPTED assignment
   before the work date (worker or business), each with a confirm
   dialog and required reason. First tell me what the backend does
   to related applications, assignments and wage obligations on
   cancel; add tests for unhandled cases.

PART D - ADMIN
D1 Workers screen: list, search by name or phone, open profile,
   verify / unverify, suspend. Field edits and password reset stay
   in Django admin: configure its ModelAdmin with search, filters
   and those actions.
D2 Dashboard screen with simple bar visuals: jobs by status,
   businesses verified (target 20), workers verified (target 50),
   paid work days (target 100), open disputes. Reuse or extend the
   pilot-summary endpoint.
D3 Disputes queue: list every "لم أستلم أجري" dispute with the
   obligation, both parties and the reason; resolve as paid or
   not paid with required notes. Reuse the disputes backend.

PART E - APP-WIDE
E1 Public endpoint /app/config/ (no auth) returning
   min_supported_version_code, latest_version_code,
   apk_download_url and support_whatsapp, all from env vars.
   Below minimum: a blocking "حدّث التطبيق" screen with the link.
   Below latest: a dismissible prompt.
E2 "تواصل مع الدعم" button on each home screen, opening WhatsApp
   to support_whatsapp.

PART F - REFERENCE DOC
Regenerate docs/WATHB_COMPLETE_REFERENCE.md from the actual code.
Every claim must be checkable against a file. Mark anything you
could not verify as "غير مُتحقق منه". The old doc had wrong test
counts, a wrong wage state machine and a CSP claim with no code.

PART G - SPONSORSHIP (PLAN ONLY, NO CODE)
Design wage sponsorship on top of the existing funding app.
Rules:
- The platform never holds money. The sponsor's budget is a ledger
  (available / reserved / allocated / spent, summing to total).
  Real payments happen outside and are only recorded.
- The worker always gets the full wage from the business.
- Program: sponsor name, total budget, currency (= job currency),
  date window, coverage in basis points, cap per worker-day,
  eligibility (business whitelist / job category / city), max
  sponsored days per worker per month, max budget and max
  outstanding per business, funds_confirmed set by admin.
- Sponsored jobs are open to VERIFIED workers only.
- Publish: reserve under select_for_update; if insufficient,
  publish as non-sponsored and tell the business beforehand.
- Acceptance: freeze sponsorship terms in terms_snapshot.
- After the worker's acknowledgement: auto-create an entitlement
  = min(wage * coverage, cap), rounded down.
- Weekly payout batches with a CSV statement; marked paid, then
  the recipient acknowledges.
- Cancellation or no-show releases; an open dispute freezes.
- Fields, not hardcoded: recipient (BUSINESS now, WORKER later),
  executor (SPONSOR now; IMPLEMENTING_PARTNER or PSP later).
- Flag for admin: repeated same worker-business pairs, wages at or
  just above the cap, a business near its outstanding limit.
- No funder login or UI. Admin manages programs and batches. App:
  "مدعوم من [الجهة]" badge, and expected / pending / paid
  reimbursement shown to the business.
Output: rules mapped to existing code with file:line, gaps,
migrations and state machines, test list, effort per piece.
[STOP] Wait for my approval.

OUT OF SCOPE - do not build
Favorite or rehire-directly lists, push notifications, payment
gateway, funder UI, offline cache, ratings with stars.E3 Biometric, only when the device supports it and the user opts
   in after a successful password login:
   - Quick unlock on app open with androidx.biometric
     BiometricPrompt, device PIN or pattern as fallback.
   - Required before "تأكيد استلام الأجر".
   - Devices without biometrics work exactly as today.
   - Tokens stay in encrypted storage. Biometrics unlock the app
     locally and never replace server authentication.WATHB WORK ORDER #3 - UI AND FLOW UPGRADE

PREREQUISITE
Work Order #2 must be complete. Read docs/WORK_ORDER_2_STATUS.md
first. If any item this order depends on (B2, B3, C3, C4, D3) is
not done, stop and tell me.

REFERENCE IMAGE
The attached image shows 8 mockup screens of another app named
"يوميتي". Use it for LAYOUT AND FLOW ONLY:
- Keep the Wathb identity: Deep Teal #006D6B, Teal #0EA5A3,
  Gold #F4B432, Warm Cream #FEF3D7, Thamanyah Sans, letterSpacing 0.
- Never use gold as text on white.
- Do not copy its name, colors, photos or wording.

PROTOCOL (same as Work Order #2)
1. Save this message verbatim as docs/WORK_ORDER_3.md and commit.
2. Strictly in order, one commit per item, both test suites after
   each item, tests for new behavior.
3. Append each item's status to docs/WORK_ORDER_3_STATUS.md
   (done / partial + what is missing / skipped + reason, hash).
4. STOP only at points marked [STOP].
5. All UI text Arabic, RTL. Report to me in Arabic.
6. The server decides every state and every rule. The app only
   renders what the server returns. No business logic in Compose.
7. Push at the end and confirm the live API reflects the changes.

H1 BOTTOM NAVIGATION, per role
- Worker: الرئيسية · شغلي · الإشعارات · حسابي
  ("شغلي" merges my applications and my assignments, with tabs).
- Business: وظائفي · المهام · الإشعارات · حسابي
- Admin: اللوحة · المنشآت · العمال · الاعتراضات
- Back stack must behave correctly per tab.

H2 IN-APP NOTIFICATIONS (no push)
- Bell with unread count in the top bar; notifications screen;
  mark one / all read; tapping opens the related object.
- First list which events create Notification rows today. Make
  sure these exist: offer received, application rejected, job
  cancelled, assignment cancelled, attendance or completion
  waiting for my confirmation, payment declared, dispute resolved.
- Refresh on app open, on resume and on pull-to-refresh.

H3 JOB CARD REDESIGN (worker home)
- Wage as the largest element. Category illustration. Title,
  business name, area, date, hours.
- "جديد" badge if published in the last 24 hours (server field).
- Business track record line from B3.
[STOP] After H3, install a debug build and tell me exactly which
screen to open so I can check the look on my device before you
continue.

H4 QUICK FILTERS on worker home
Chips: اليوم · غداً · area (neighborhoods of the pilot city, a
fixed server-side list) · category chips from C3. Filtering is
done by the server via query params.

H5 JOB DETAIL REDESIGN
- Header with category illustration, title, business.
- Three stat tiles: wage, hours, date.
- Location section (from H6), description, requirements as a
  checklist, fixed apply button at the bottom.

H6 JOB LOCATION
- Optional latitude/longitude on the job, set by the business
  with a pin picker at creation.
- Worker sees a small map and "افتح في الخرائط" (geo: intent).
- Never store or request the worker's location.
- Prefer a library that needs no API key (e.g. osmdroid /
  OpenStreetMap). [STOP] If you think Google Maps is required,
  ask me first; it needs a key and billing.

H7 WORKER DECLARATION AT OFFER ACCEPTANCE
- Before accepting an OFFERED assignment, three required
  checkboxes: أوافق على الشروط المذكورة · ألتزم بالحضور في الموعد ·
  أؤكد أنني قادر على تنفيذ العمل.
- The server rejects accept without them, and stores the
  declaration version and accepted_at alongside terms_snapshot.
- Tests for the rejection.

H8 STATUS TIMELINE
- Steps: تقديم الطلب ← عرض من صاحب العمل ← قبولك ← تسجيل الحضور ←
  تأكيد الإنجاز ← إعلان الدفع ← استلام الأجر
- The SERVER returns a computed `timeline` field (steps with
  done / current / pending, plus cancelled / disputed markers).
  The app only draws it.
- Shown on every item in "شغلي" and on the business assignment
  detail.

H9 CONFIRMATION SCREENS
- After applying: "تم إرسال طلبك" + "بانتظار رد صاحب العمل"
  (never "تم قبول طلبك" at this point) + the timeline.
- After accepting an offer: "تم تأكيد المهمة" with date, time,
  wage, location, open in maps, call and WhatsApp buttons.

H10 "هناك مشكلة" WITH STRUCTURED REASONS
- Wherever a worker can dispute (attendance, completion, payment):
  a reason list plus an optional note: الأجر أقل من المتفق ·
  الشغل تغيّر عن المتفق · لم يُحسب يوم عمل · لم أستلم الأجر · غير ذلك.
- Map each reason to the existing dispute categories server-side.
  Show the reason in the admin disputes queue (D3).

H11 WAGE ACKNOWLEDGEMENT AND RECEIPT
- Before acknowledging: "استلمت الأجر كامل (X شيكل)؟"
  نعم -> acknowledge. لا -> dispute with reason "الأجر أقل من المتفق".
- After acknowledging: a receipt screen (job, date, wage, payment
  method, status "تم الدفع") and one question:
  "وصلك الأجر في الموعد المتفق؟" نعم / لا, stored on the obligation.
- Update B3: on-time uses the worker's answer when present,
  otherwise the payment deadline rule.

H12 VISUAL PASS
- Original simple vector illustrations per job category and for
  success, empty and error states, in Wathb colors.
- Consistent spacing, card style, typography scale across all
  screens. Check WCAG AA contrast on every new color pair.

OUT OF SCOPE - do not build
Work timer or pause button, in-app chat, photo upload on
completion, worker profile photos, star ratings, GPS tracking or
"near me" by GPS, push notifications.
