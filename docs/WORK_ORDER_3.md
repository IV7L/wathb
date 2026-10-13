WATHB WORK ORDER #3 - UI AND FLOW UPGRADE

PROTOCOL AMENDMENT — 2026-10-12 (supersedes original item 2)
Testing rules:
- After each item: run ONLY the Django tests of the apps you touched,
  and only if the item changed the backend. Skip Django tests if the
  item is Android-only or docs-only.
- Write new tests ONLY for: money, state transitions,
  permissions/roles, auth/session, phone normalization, and anything
  a [STOP] point covers. No new tests for pure UI or styling changes.
- At the end of each Part: run the full Django suite, the full Android
  JVM suite, and assembleDebug. Do not start the next Part until all
  pass. If something fails, find the breaking commit and fix it in a
  separate commit.
- One commit per item remains mandatory.

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

PROTOCOL (same as Work Order #2, amended above)
1. Save this message verbatim as docs/WORK_ORDER_3.md and commit.
2. Strictly in order, one commit per item, tests per amendment above.
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
- Job location is required; the business sets it with a pin
  picker (drag the map under a fixed center pin; "موقعي الحالي"
  may use the BUSINESS device location).
- Worker home has a list / map toggle; each job is a pin showing
  the wage; tap opens a small card, then the job detail; same
  filters as H4; cluster when crowded.
- Job detail: small map with the pin and "افتح في الخرائط"
  (geo: intent with coordinates and job title).
- Server validates coordinates inside a pilot bounding box.
- Worker location is allowed on the device only (map tab
  "موقعي" button): moves the map and computes distance locally;
  never sent to our server or any third party; everything works
  when permission is denied.
- Tests: coordinate validation; no endpoint accepts worker
  coordinates.

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
