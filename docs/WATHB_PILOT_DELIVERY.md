# وَثْب (Wathb) — Pilot Delivery Report

**Date:** 2026-10-11  
**Branch:** main  
**APK:** `android/app/build/outputs/apk/debug/app-debug.apk`

---

## Summary

All 8 phases of the Wathb brand integration and live-pilot readiness task are complete. The platform is renamed from يوميّاتي to **وَثْب**, carries the full Wathb visual identity, and all previously identified BLOCKER/HIGH security findings are resolved.

---

## Phase Results

### Phase 1 — Brand Asset Package ✓

Created `docs/brand/wathb/` with:

| File | Description |
|------|-------------|
| `BRAND_GUIDELINES.md` | Voice, tone, logo usage rules |
| `COLORS.md` | Full palette with hex, RGB, accessibility ratios |
| `LOGO_CONCEPT.md` | Symbol anatomy, construction grid, file inventory |
| `DESIGN_TOKENS.md` | Kotlin/CSS token reference for all visual values |
| `assets/wathb-symbol.svg` | Full-color symbol (leaping figure + wave + sun) |
| `assets/wathb-wordmark-ar.svg` | Arabic wordmark وَثْب |
| `assets/wathb-wordmark-en.svg` | English wordmark Wathb |
| `assets/wathb-lockup.svg` | Horizontal lockup (symbol + both wordmarks) |
| `assets/wathb-monochrome.svg` | Single-color version |
| `assets/wathb-icon-preview.svg` | App icon at launcher resolution (108×108) |

### Phase 2 — Design System ✓

| File | Change |
|------|--------|
| `Color.kt` | Replaced green palette with Wathb palette (WathbDeepTeal #006D6B primary, WathbGold #F4B432 secondary, WathbTeal #0EA5A3 tertiary) |
| `Theme.kt` | Updated LightColorScheme + DarkColorScheme with full Wathb Material3 mapping; added `WathbShapes` to MaterialTheme |
| `Type.kt` | Added `bodySmall` and `labelSmall`; set `letterSpacing = 0.sp` on all styles for Arabic RTL |
| `Shape.kt` | New: WathbShapes (extraSmall=4dp → extraLarge=24dp) |
| `Spacing.kt` | New: WathbSpacing (xs=4dp, sm=8dp, md=16dp, lg=24dp, xl=32dp) |

### Phase 3 — Screen Brand Application ✓

| Screen | Change |
|--------|--------|
| `LoginScreen.kt` | Brand name: "يوميّاتي" → "وَثْب"; tagline: "سوق العمل اليومي في غزة" → "خطوتك للشغل الجاي." |
| `BusinessHomeScreen.kt` | Verification message: "فريق يوميّاتي" → "فريق وَثْب" |
| All other screens | Colors apply automatically through MaterialTheme; no hardcoded brand references remain |

### Phase 4 — App Identity ✓

| Change | Detail |
|--------|--------|
| `strings.xml` | `app_name` → "وَثْب"; new `app_tagline` string resource |
| `ic_launcher_background.xml` | Background color: `#2E7D32` (old green) → `#006D6B` (Wathb Deep Teal) |
| `ic_launcher_foreground.xml` | Replaced placeholder double-circle with Wathb leaping figure: white figure + gold sun + teal wave on 108dp viewport |

### Phase 5 — Pilot Readiness Security Fixes ✓

#### B-1: Production Settings (BLOCKER resolved)

- **Created** `backend/yawmiyati/settings/production.py` with `DEBUG=False`, HSTS, SSL redirect, `X_FRAME_OPTIONS=DENY`, `CORS_ALLOW_ALL_ORIGINS=False`, no hardcoded secrets.
- **Updated** `backend/yawmiyati/wsgi.py` and `backend/yawmiyati/asgi.py`: default settings module changed from `yawmiyati.settings.development` → `yawmiyati.settings.production`.
- **Verified** with `python manage.py check --deploy --settings=yawmiyati.settings.production`: **0 security ERRORs**. Pre-existing drf_spectacular schema warnings are not security issues.

#### B-2: Environment Documentation (BLOCKER resolved)

- **Created** `backend/.env.example` with all 14 variable names and safe placeholder values.
- Comments in the file emphasize: never commit `.env` to VCS, use a 50+ character random `DJANGO_SECRET_KEY`.

#### H-1: Token Expiry in Wages ViewModels (HIGH resolved)

Added `_sessionExpired: MutableStateFlow<Boolean>` to all three wage ViewModels:

| ViewModel | Changes |
|-----------|---------|
| `WagesViewModel` | `sessionExpired` StateFlow; 401 on load/declarePayment → navigate to login |
| `WorkerWagesViewModel` | `sessionExpired` StateFlow; 401 on load/acknowledgePayment → navigate to login |
| `AdminWagesViewModel` | `sessionExpired` StateFlow; 401 on load/verifyDeclaration → navigate to login |

Screens updated to accept `onNavigateToLogin: () -> Unit`:
- `WageObligationsScreen.kt`
- `WorkerWagesScreen.kt`
- `AdminWagesScreen.kt`

`NavGraph.kt` wires `onNavigateToLogin = { navController.navigate(Login) { popUpTo(0, inclusive=true) } }` for all three wage destinations.

**5 regression tests added** to `WagesViewModelTest.kt` (test 18–22 of 30 total):
- `WagesViewModel auth error on load sets sessionExpired`
- `WagesViewModel auth error on declarePayment sets sessionExpired`
- `WorkerWagesViewModel auth error on acknowledgePayment sets sessionExpired`
- `AdminWagesViewModel auth error on load sets sessionExpired`
- `non-auth error does NOT set sessionExpired`

#### H-2: Atomic Wage Operations (HIGH resolved)

Added `from django.db import transaction` and:

| View | Change |
|------|--------|
| `DeclarePaymentView.post` | `@transaction.atomic` + `select_for_update()` on obligation fetch |
| `AcknowledgePaymentView.post` | `@transaction.atomic` + `select_for_update()` on obligation fetch |
| `AdminVerifyPaymentView.post` | `@transaction.atomic` + `select_for_update()` on obligation AND declaration fetch |

Race condition window eliminated. Concurrent duplicate declarations/acknowledges will now queue rather than produce inconsistent state.

#### H-3: Backup Documentation (HIGH resolved)

- **Created** `docs/operations/backup-and-restore.md`: pg_dump procedure, automated cron backup, restore procedure, media files backup, RPO/RTO targets, secret rotation instructions.

### Phase 6 — Tests ✓

| Test Suite | Result |
|------------|--------|
| Django wages tests | **29/29 PASS** (`python manage.py test wages --keepdb`) |
| Android JVM unit tests | **30/30 PASS** (`./gradlew testDebugUnitTest`) — 18 original + 5 selectReviewableDeclaration + 5 H-1 sessionExpired + 2 pre-existing others |
| Django deploy check | **0 ERRORs** (`manage.py check --deploy --settings=…production`) |
| Debug APK build | **BUILD SUCCESSFUL** (`./gradlew assembleDebug`) |

### Phase 7 — Pilot Config Docs ✓

- **Created** `docs/operations/pilot-setup.md`: server setup, environment config, migrations, Gunicorn + nginx, Android APK build, user role creation, business verification workflow, monitoring, shutdown.

### Phase 8 — This Report ✓

---

## Remaining Known Warnings (Pre-existing, Not Blockers)

| ID | Finding | Status |
|----|---------|--------|
| drf_spectacular.W001/W002 | OpenAPI schema inference warnings on APIViews without explicit serializer declarations | Pre-existing, not security issues; fix is out of scope |
| security.W009 | Secret key < 50 chars in deploy check run | Expected (test key used); production `.env` must use `python -c "import secrets; print(secrets.token_hex(50))"` |

---

## File Change Summary

```
Created (16):
  docs/brand/wathb/BRAND_GUIDELINES.md
  docs/brand/wathb/COLORS.md
  docs/brand/wathb/LOGO_CONCEPT.md
  docs/brand/wathb/DESIGN_TOKENS.md
  docs/brand/wathb/assets/wathb-symbol.svg
  docs/brand/wathb/assets/wathb-wordmark-ar.svg
  docs/brand/wathb/assets/wathb-wordmark-en.svg
  docs/brand/wathb/assets/wathb-lockup.svg
  docs/brand/wathb/assets/wathb-monochrome.svg
  docs/brand/wathb/assets/wathb-icon-preview.svg
  android/.../ui/theme/Shape.kt
  android/.../ui/theme/Spacing.kt
  backend/yawmiyati/settings/production.py
  backend/.env.example
  docs/operations/backup-and-restore.md
  docs/operations/pilot-setup.md

Modified (15):
  android/.../ui/theme/Color.kt           (Wathb palette)
  android/.../ui/theme/Theme.kt           (Wathb color scheme + shapes)
  android/.../ui/theme/Type.kt            (letterSpacing, bodySmall, labelSmall)
  android/.../screens/auth/LoginScreen.kt (وَثْب brand + new tagline)
  android/.../screens/business/BusinessHomeScreen.kt (فريق وَثْب)
  android/.../res/values/strings.xml      (app_name + tagline resource)
  android/.../res/drawable/ic_launcher_background.xml (#006D6B)
  android/.../res/drawable/ic_launcher_foreground.xml (leaping figure)
  android/.../viewmodels/WagesViewModel.kt           (sessionExpired)
  android/.../viewmodels/WorkerWagesViewModel.kt     (sessionExpired)
  android/.../viewmodels/AdminWagesViewModel.kt      (sessionExpired)
  android/.../screens/business/WageObligationsScreen.kt (onNavigateToLogin)
  android/.../screens/worker/WorkerWagesScreen.kt    (onNavigateToLogin)
  android/.../screens/admin/AdminWagesScreen.kt      (onNavigateToLogin)
  android/.../navigation/NavGraph.kt                 (wire onNavigateToLogin)
  backend/wages/views.py                             (@transaction.atomic)
  backend/yawmiyati/wsgi.py                          (production default)
  backend/yawmiyati/asgi.py                          (production default)
  android/app/src/test/.../WagesViewModelTest.kt     (+5 H-1 regression tests)
```

---

## Pre-Pilot Checklist

Before directing pilot users to the app:

- [ ] Generate real `DJANGO_SECRET_KEY` (50+ chars)
- [ ] Fill all variables in `.env` — do not leave any placeholder values
- [ ] Run `python manage.py check --deploy` — confirm 0 ERRORs
- [ ] Run `python manage.py migrate` on production database
- [ ] Run `python manage.py collectstatic`
- [ ] Configure nginx with TLS (Let's Encrypt recommended)
- [ ] Run `python manage.py createsuperuser` and set role=ADMIN
- [ ] Install and test APK on pilot devices via `adb install`
- [ ] Verify `adb reverse tcp:8000 tcp:8000` is NOT used (use real HTTPS URL in production)
- [ ] Set up daily backup cron (`docs/operations/backup-and-restore.md`)
