# WORK ORDER 2 — Status Log

| Item | Status | Commit | Notes |
|------|--------|--------|-------|
| 0.1 | done | d6100b2 | build.gradle.kts committed, worktree verified |
| 0.2 | done | ff6d699 | termsSnapshot in AssignmentResponse; agreed terms shown on worker card |
| 0.3 | done | 05125a4 | Confirm button hidden for submitter; "بانتظار تأكيد الطرف الآخر" shown |
| 0.4 | done | 1e37b28 | 3 MockWebServer authenticator tests; TokenAuthenticator extracted |
| 0.5 | done | 8e37b00 | PaymentMethod choices on wages model + migration + serializer validation + 3 new tests |
| 0.6 | done | 9bc7c1e | ensure_admin command (5 tests) + backend/build.sh |
| A1 | done | 236c2ac | Phone login: normalize_phone, PhoneAuthBackend, JWT serializer, throttles, migration, admin reset action, Android UI + routes updated; 159 Django + 35 JVM tests pass |
| A2 | done | 236c2ac | Already implemented — RegisterSerializer.validate() rejects non-WORKER/BUSINESS; ADMIN/FUNDER both return 400; 2 tests in test_auth.py |
| A3 | done | 9bc7c1e | Already implemented at 0.6 — ensure_admin uses normalize_phone(); invalid phone warning + skip; 5 tests |
| A4 | done | 34553df | HMAC-SHA256 national ID: normalize_national_id + hash_national_id in utils.py; WorkerProfile.set_national_id(); ImproperlyConfigured startup check in production.py; 17 new tests. No existing rows; field dormant (no API/Android collection yet) |
| A5 | done | 85791e0 | Consent screen: accounts/consent.py (CONSENT_VERSION=1, Arabic text); User.consent_version + accepted_at; RegisterSerializer validates version; ConsentScreen.kt (checkbox + scroll); Login→Consent→Register flow; 3 new tests; 179 Django + 35 JVM pass |
