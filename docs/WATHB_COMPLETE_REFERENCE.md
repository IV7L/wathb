# وَثْب — Wathb · المرجع الشامل للمشروع

> **خطوتك للشغل الجاي.**  
> سوق عمل يومي في غزة يربط العمال بأصحاب العمل بطريقة موثّقة وشفافة.

---

## فهرس المحتويات

1. [نظرة عامة على المشروع](#1-نظرة-عامة-على-المشروع)
2. [الهوية البصرية — وَثْب](#2-الهوية-البصرية--وَثْب)
3. [معمارية النظام](#3-معمارية-النظام)
4. [Backend — Django](#4-backend--django)
5. [Android App — Kotlin/Compose](#5-android-app--kotlincompose)
6. [نموذج البيانات](#6-نموذج-البيانات)
7. [API Endpoints](#7-api-endpoints)
8. [المصادقة والصلاحيات](#8-المصادقة-والصلاحيات)
9. [دورة حياة الوظيفة والأجور](#9-دورة-حياة-الوظيفة-والأجور)
10. [الإعدادات والبيئات](#10-الإعدادات-والبيئات)
11. [الاختبارات](#11-الاختبارات)
12. [النشر — Render](#12-النشر--render)
13. [الأمان](#13-الأمان)
14. [النسخ الاحتياطي والاستعادة](#14-النسخ-الاحتياطي-والاستعادة)
15. [حالة المشروع وسجل التغييرات](#15-حالة-المشروع-وسجل-التغييرات)

---

## 1. نظرة عامة على المشروع

### الهدف

منصة رقمية تتيح للعمال في غزة إيجاد وظائف يومية مشروعة، فهم شروطها وأجورها قبل القبول، وتتبع حالة دفع أجورهم. أصحاب العمل ينشرون الوظائف ويختارون العمال ويوثّقون عمليات الدفع. الممولون يدعمون المشاريع المعتمدة.

### المبدأ التوجيهي

> العامل يجب أن يعرف ما يوافق عليه، وماذا يجب أن يفعل، وكم يجب أن يكسب، وهل تم دفع أجره.

### سياق الـ Pilot

- **المنطقة:** خان يونس، غزة
- **المدة:** 30 يوماً
- **الأهداف:** 20 منشأة موثّقة، 50 عامل موثّق، 100 يوم عمل مدفوع
- **حالة المشروع:** مرحلة Pilot مغلق جاهز للنشر

### الأدوار الأربعة

| الدور | Arabic | الوصف |
|-------|--------|-------|
| `WORKER` | عامل | يبحث عن وظائف ويتقدم ويُقرّ استلام الأجر |
| `BUSINESS` | منشأة | تنشر وظائف وتختار عمال وتُعلن دفع الأجور |
| `FUNDER` | ممول | يموّل المقترحات المعتمدة ويتابع الاستخدام |
| `ADMIN` | مدير | يُحقق الحسابات ويراجع النزاعات والمدفوعات |

---

## 2. الهوية البصرية — وَثْب

### الاسم والشعار الشعاري

| العنصر | القيمة |
|--------|--------|
| الاسم العربي | **وَثْب** |
| الاسم الإنجليزي | **Wathb** |
| المعنى | القفز / الوثبة — حركة للأمام |
| الشعار الشعاري | **خطوتك للشغل الجاي.** |
| الترجمة | Your step toward tomorrow's work. |

### نظام الشعار

```
assets/wathb-symbol.svg          شكل إنساني يقفز + موجة تيل + شمس ذهبية
assets/wathb-wordmark-ar.svg     وَثْب (خط ثقيل)
assets/wathb-wordmark-en.svg     Wathb (خط مطابق)
assets/wathb-lockup.svg          الثلاثة مجتمعة
assets/wathb-monochrome.svg      نسخة أحادية اللون
assets/wathb-icon-preview.svg    معاينة الأيقونة
```

### لوحة الألوان

| الاسم | HEX | الاستخدام |
|-------|-----|-----------|
| **Deep Teal** | `#006D6B` | اللون الرئيسي، الأزرار، الهيدر |
| **Teal** | `#0EA5A3` | ثانوي، التفاعل، الموجة |
| **Gold** | `#F4B432` | تمييز، شمس الشعار، الشارات |
| **Warm Cream** | `#FEF3D7` | خلفيات، أسطح البطاقات |
| Deep Teal Dark | `#004F4E` | الحالة المضغوطة، الوضع الداكن |
| Teal Light | `#B2E8E7` | Primary container |
| Gold Dark | `#C48A14` | الحالة المضغوطة للذهبي |
| Error Red | `#B3261E` | حالات الخطأ |

**إمكانية الوصول:** جميع مجموعات النص/الخلفية تحقق WCAG 2.1 AA (4.5:1 للنص العادي).  
⚠️ لا تستخدم الذهبي `#F4B432` كلون نص على خلفيات بيضاء — النسبة 2.3:1 فقط.

### الخط — ثمانية Sans (Thamanyah Sans)

```
res/font/thmanyahsans_light.otf    FontWeight.Light
res/font/thmanyahsans_regular.otf  FontWeight.Normal
res/font/thmanyahsans_medium.otf   FontWeight.Medium
res/font/thmanyahsans_bold.otf     FontWeight.Bold
res/font/thmanyahsans_black.otf    FontWeight.Black
```

- يُطبَّق على كل 15 نمط نصي في Material3
- `letterSpacing = 0.sp` على جميع الأنماط (ضروري للعربية)
- مُعرَّف كـ `ThamanyahSans FontFamily` في `Type.kt`

---

## 3. معمارية النظام

```
┌──────────────────────────────────────────────┐
│  تطبيق Android (Kotlin / Jetpack Compose)    │
│  عربي RTL · Material3 · minSdk 26           │
│  Retrofit + OkHttp + EncryptedSharedPrefs   │
└────────────────────┬─────────────────────────┘
                     │ HTTPS · JSON REST API
                     │ Bearer JWT
┌────────────────────▼─────────────────────────┐
│  Django 5.1.4 + DRF (Python 3.12)           │
│  JWT · Role permissions · State machines     │
│  Gunicorn 23 (3 workers) · production.py     │
└────────────────────┬─────────────────────────┘
                     │ psycopg2
┌────────────────────▼─────────────────────────┐
│  PostgreSQL 14                               │
│  ACID · Row-level locking (select_for_update) │
└──────────────────────────────────────────────┘
```

### بنية الـ Backend

```
backend/
├── yawmiyati/         إعدادات المشروع (base / dev / test / production)
├── accounts/          نموذج المستخدم، الأدوار، الملفات الشخصية
├── businesses/        تسجيل وتحقق المنشآت
├── jobs/              الوظائف، الطلبات، العروض، آلات الحالة
├── assignments/       لقطة الشروط المقبولة، الحضور، الإنجاز
├── wages/             التزامات الأجور، إعلانات الدفع، الأدلة
├── disputes/          دورة حياة النزاعات وحلها
├── funding/           مقترحات التمويل، الميزانيات، التخصيصات
├── notifications/     نموذج الإشعارات داخل التطبيق
├── audit/             سجل أحداث غير قابل للتعديل
└── reporting/         تقارير مجمّعة وتصدير CSV
```

---

## 4. Backend — Django

### المكتبات (requirements.txt)

```
Django==5.1.4
djangorestframework==3.15.2
djangorestframework-simplejwt==5.3.1
drf-spectacular==0.27.2
django-filter==24.3
django-cors-headers==4.6.0
psycopg2-binary==2.9.10
python-decouple==3.8
Pillow==11.0.0
python-magic==0.4.27
gunicorn==23.0.0
```

### الإعدادات — ملفات متعددة

| الملف | يُستخدم متى |
|-------|------------|
| `settings/base.py` | القواعد المشتركة |
| `settings/development.py` | التطوير المحلي (`DEBUG=True`) |
| `settings/test.py` | تشغيل الاختبارات |
| `settings/production.py` | Render / أي خادم إنتاج |

### إعدادات الإنتاج المهمة (`production.py`)

```python
DEBUG = False
SECURE_HSTS_SECONDS = 31536000
SECURE_HSTS_INCLUDE_SUBDOMAINS = True
SECURE_HSTS_PRELOAD = True
SECURE_SSL_REDIRECT = True
SECURE_PROXY_SSL_HEADER = ("HTTP_X_FORWARDED_PROTO", "https")
SESSION_COOKIE_SECURE = True
CSRF_COOKIE_SECURE = True
SECURE_CONTENT_TYPE_NOSNIFF = True
X_FRAME_OPTIONS = "DENY"
CORS_ALLOW_ALL_ORIGINS = False

# SSL لقاعدة البيانات (Render يتطلبه)
DATABASES["default"]["OPTIONS"] = {"sslmode": config("DB_SSLMODE", default="require")}

# تسجيل الأخطاء إلى stdout (Render يلتقطه)
LOGGING → StreamHandler → level=ERROR
```

### متغيرات البيئة المطلوبة

```bash
DJANGO_SECRET_KEY              # ≥ 50 حرف عشوائي
DJANGO_DEBUG                   # False
DJANGO_ALLOWED_HOSTS           # wathb-api.onrender.com
DJANGO_SETTINGS_MODULE         # yawmiyati.settings.production
DB_NAME                        # اسم قاعدة البيانات
DB_USER                        # اسم مستخدم PostgreSQL
DB_PASSWORD                    # كلمة مرور قوية
DB_HOST                        # hostname من Render
DB_PORT                        # 5432
DB_SSLMODE                     # require (افتراضي) أو disable للاتصالات الداخلية
JWT_ACCESS_TOKEN_LIFETIME_MINUTES  # 15
JWT_REFRESH_TOKEN_LIFETIME_DAYS    # 7
MEDIA_ROOT                     # مسار تخزين الملفات
MAX_UPLOAD_SIZE_BYTES          # 5242880 (5 MB)
CORS_ALLOWED_ORIGINS           # https://wathb-api.onrender.com
STATIC_ROOT                    # مسار الملفات الثابتة
```

### أوامر النشر (بالترتيب)

```bash
pip install -r requirements.txt
python manage.py migrate --settings=yawmiyati.settings.production
python manage.py collectstatic --settings=yawmiyati.settings.production --noinput
python manage.py check --deploy --settings=yawmiyati.settings.production
gunicorn yawmiyati.wsgi:application --workers 3 --bind 0.0.0.0:$PORT --timeout 30
```

---

## 5. Android App — Kotlin/Compose

### المواصفات التقنية

| العنصر | القيمة |
|--------|--------|
| Language | Kotlin 2.0.20 |
| UI | Jetpack Compose + Material3 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| AGP | 8.5.2 |
| Gradle | 8.7 |
| Application ID | `com.yawmiyati.app` |
| Version | 1.0.0 (versionCode 1) |

### المكتبات الرئيسية

| المكتبة | الإصدار | الاستخدام |
|---------|---------|-----------|
| Retrofit | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | Networking |
| Gson | 2.11.0 | JSON |
| Navigation Compose | 2.8.0 | التنقل بين الشاشات |
| ViewModel Compose | 2.8.5 | State management |
| DataStore | 1.1.1 | التخزين المحلي |
| Security Crypto | 1.1.0-alpha06 | EncryptedSharedPreferences |
| Coroutines | 1.8.1 | البرمجة غير المتزامنة |

### بنية URL في APK

| نوع البناء | Base URL |
|------------|----------|
| Debug | `http://localhost:8000/api/v1/` |
| Release | `https://api.yawmiyati.com/api/v1/` |

> **ملاحظة:** الـ Release URL يحتوي على `api.yawmiyati.com` — يحتاج قراراً بشأن النطاق قبل بناء APK الإنتاج.

### شاشات التطبيق

| الشاشة | Route | الدور |
|--------|-------|-------|
| `LoginScreen` | `login` | جميع الأدوار |
| `RegisterScreen` | `register` | جميع الأدوار |
| `WorkerHomeScreen` | `worker_home` | عامل |
| `WorkerAssignmentsScreen` | `worker_assignments` | عامل |
| `WorkerWagesScreen` | `worker_wages` | عامل |
| `BusinessHomeScreen` | `business_home` | منشأة |
| `JobCreateScreen` | `job_create` | منشأة |
| `WageObligationsScreen` | `business_wages` | منشأة |
| `JobDetailScreen` | `job_detail/{jobId}` | عامل |
| `JobApplicationsScreen` | `job_applications/{jobId}` | منشأة |
| `BusinessAssignmentDetailScreen` | `business_assignment/{id}/{email}` | منشأة |
| `AdminVerificationScreen` | `admin_verification` | مدير |
| `AdminWagesScreen` | `admin_wages` | مدير |

### ViewModels والـ State

```
AuthViewModel           تسجيل الدخول / التسجيل / بيانات المستخدم الحالي
JobsViewModel           قائمة الوظائف للعمال
JobCreateViewModel      إنشاء وظيفة جديدة
JobDetailViewModel      تفاصيل وظيفة + التقديم
JobApplicationsViewModel طلبات الوظيفة (منشأة)
AssignmentsViewModel    قائمة التكليفات (عامل)
BusinessAssignmentViewModel تفاصيل تكليف (منشأة)
AdminViewModel          التحقق من الحسابات (مدير)
WagesViewModel          أجور المنشأة + إعلان الدفع
WorkerWagesViewModel    أجور العامل + الإقرار
AdminWagesViewModel     مراجعة الأجور (مدير) + selectReviewableDeclaration()
```

### إدارة JWT في Android

- التوكنات مخزّنة في `EncryptedSharedPreferences` (AES256-GCM)
- `authInterceptor` في `ApiClient` يضيف `Authorization: Bearer <token>` تلقائياً
- `sessionExpired: StateFlow<Boolean>` في الـ ViewModels الثلاثة للأجور
- عند انتهاء الصلاحية (401): التوجيه التلقائي لشاشة تسجيل الدخول
- لا يُسجَّل أي توكن أو بيانات مصادقة

### تفاصيل تقنية للعربية

- `android:supportsRtl="true"` في `AndroidManifest.xml`
- `letterSpacing = 0.sp` على جميع الأنماط النصية
- جميع النصوص في `strings.xml` بالعربية

---

## 6. نموذج البيانات

> جميع المبالغ المالية مخزّنة كأعداد صحيحة في الوحدات الفرعية للعملة  
> (ILS: أغورات — 1 شيكل = 100 أغورة · USD: سنت)

### accounts

**User** — `AUTH_USER_MODEL`

| الحقل | النوع | الملاحظات |
|-------|-------|-----------|
| id | UUID PK | |
| email | EmailField unique | معرّف تسجيل الدخول |
| phone | CharField | اختياري |
| role | CharField | WORKER / BUSINESS / FUNDER / ADMIN |
| is_verified | BooleanField | يُضبط من قبل Admin |
| is_active | BooleanField | |
| is_staff | BooleanField | الوصول لـ Django admin |

**WorkerProfile** (OneToOne → User[WORKER])  
الاسم، المدينة، المنطقة، المهارات، الخبرة، `national_id_hash` (SHA-256 فقط)، التوفّر

**BusinessProfile** (OneToOne → User[BUSINESS])  
الاسم القانوني، اسم التجارة، رقم التسجيل، العنوان، جهة الاتصال، `verification_status` (PENDING/APPROVED/REJECTED/SUSPENDED)

---

### jobs

**JobListing** — آلة الحالة الكاملة

```
DRAFT → PENDING_REVIEW → PUBLISHED → FILLED → IN_PROGRESS → COMPLETED
                                            ↘ CANCELLED
```

حقول: العنوان، الوصف، الموقع، تاريخ العمل، وقت البداية/النهاية، عدد العمال المطلوب، المهارات، المبلغ/العملة/أساس الأجر (PER_DAY/PER_TASK/PER_HOUR)، طريقة الدفع (CASH/BANK_TRANSFER/OTHER)، مهلة الدفع (أيام)، شروط الحضور، سياسة الإلغاء، `is_funded`، `version` (يُزاد عند كل تعديل جوهري)

**JobApplication** (job × worker — unique constraint)  
`status`: PENDING / OFFERED / ACCEPTED / REJECTED / WITHDRAWN / EXPIRED

---

### assignments

**Assignment** (job × worker — unique constraint)  
- `terms_snapshot: JSONField` — نسخة غير قابلة للتعديل من شروط الوظيفة لحظة القبول
- `status`: OFFERED / ACCEPTED / DECLINED / ACTIVE / COMPLETED / CANCELLED

**AttendanceRecord** — `status`: SUBMITTED / CONFIRMED / DISPUTED / CANCELLED  
**CompletionRecord** (OneToOne → Assignment) — `status`: SUBMITTED / CONFIRMED / DISPUTED

---

### wages

**WageObligation** (OneToOne → Assignment — يمنع التكرار)

```
PENDING → DUE → AWAITING_ACK ──→ VERIFIED_PAID   (نقدي: إقرار العامل)
                              ↘
                               UNDER_REVIEW → VERIFIED_PAID  (بنكي: تأكيد Admin)
                                           ↘ REJECTED
                              ↘ DISPUTED
```

**PaymentDeclaration** (FK → WageObligation، يمكن تعدد الإعلانات)  
`status`: SUBMITTED / ACKNOWLEDGED / DISPUTED / VERIFIED / REJECTED  
`reviewed_by`, `reviewed_at`, `review_notes` — يُملأ من Admin

**PaymentEvidence** (FK → PaymentDeclaration)  
- ملف خاص (خارج الدليل العام)
- أنواع مسموحة: `image/jpeg`, `image/png`, `application/pdf`
- حجم أقصى: 5 MB
- اسم الملف على القرص بـ UUID (لا باسم المستخدم)
- التحقق من نوع الملف بقراءة magic bytes لا من HTTP header

---

### disputes

**Dispute** — الفئات: WORKER_NO_SHOW / ATTENDANCE_DISPUTED / WORK_INCOMPLETE / WAGE_DISPUTED / PAYMENT_NOT_RECEIVED / EVIDENCE_INSUFFICIENT / CONDITIONS_CHANGED / CANCELLATION / OTHER  
`status`: OPEN / IN_REVIEW / RESOLVED / CLOSED

**DisputeEvent** — سجل أحداث النزاع (لا يُحذف)

---

### funding

**FundingProposal** — `status`: PROPOSED / PLEDGED / APPROVED / ACTIVE / COMPLETED / CANCELLED

**FundingBudget** (OneToOne → FundingProposal)  
```
reserved_amount + allocated_amount + spent_amount ≤ total_amount
```
`SELECT FOR UPDATE` يمنع الإنفاق الزائد المتزامن

**FundingAllocation** (OneToOne → JobListing)  
`status`: RESERVED / ALLOCATED / RELEASED / SPENT

---

### audit

**AuditEvent** — غير قابل للحذف أو التعديل  
`actor`, `action`, `model_name`, `object_id`, `details (JSON)`, `timestamp`, `ip_address`

---

### notifications

**Notification**  
`notification_type`, `title`, `body`, `related_model`, `related_id`, `is_read`

---

## 7. API Endpoints

**Base URL (Render):** `https://wathb-api.onrender.com/api/v1/`  
**Base URL (محلي):** `http://localhost:8000/api/v1/`  
**Android Emulator:** `http://10.0.2.2:8000/api/v1/`

### الاتفاقيات

- كل الـ endpoints (ما عدا register + login) تتطلب: `Authorization: Bearer <access_token>`
- صفحة التخزين الافتراضية: 20 عنصراً (`?page=2&page_size=50`)
- تصفية عبر query params: `?status=PUBLISHED&city=khan_younis&ordering=-work_date`

### جدول الـ Endpoints

```
# المصادقة
POST   /auth/register/
POST   /auth/login/
POST   /auth/token/refresh/
POST   /auth/logout/
GET    /auth/me/
PATCH  /auth/me/

# العمال
GET    /workers/profile/
PATCH  /workers/profile/

# المنشآت
GET    /businesses/profile/
PATCH  /businesses/profile/
GET    /admin/businesses/                         (admin)
PATCH  /admin/businesses/{id}/verify/            (admin)
PATCH  /admin/businesses/{id}/suspend/           (admin)

# الوظائف
GET    /jobs/                                     (عامل: منشورة · منشأة: خاصة بها)
POST   /jobs/
GET    /jobs/{id}/
PATCH  /jobs/{id}/
DELETE /jobs/{id}/                               (draft فقط)
POST   /jobs/{id}/submit/                        (DRAFT → PENDING_REVIEW)
POST   /jobs/{id}/publish/
POST   /jobs/{id}/cancel/
GET    /jobs/{id}/applications/
POST   /jobs/{id}/apply/
PATCH  /jobs/{id}/applications/{app_id}/         (قبول/رفض العرض)
DELETE /jobs/{id}/applications/{app_id}/         (سحب الطلب)

# التكليفات
GET    /assignments/
GET    /assignments/{id}/
POST   /assignments/{id}/accept/
POST   /assignments/{id}/decline/
POST   /assignments/{id}/cancel/
POST   /assignments/{id}/attendance/
GET    /assignments/{id}/attendance/
PATCH  /assignments/{id}/attendance/{rec_id}/confirm/
PATCH  /assignments/{id}/attendance/{rec_id}/dispute/
POST   /assignments/{id}/completion/
PATCH  /assignments/{id}/completion/confirm/
PATCH  /assignments/{id}/completion/dispute/

# الأجور
GET    /wages/                                   (عامل: خاصة به · منشأة: خاصة بها)
GET    /wages/{id}/
POST   /wages/{id}/declare-payment/             (منشأة)
POST   /wages/{id}/acknowledge/                 (عامل)
POST   /wages/{id}/dispute-payment/             (عامل)
POST   /wages/{id}/declarations/{dec_id}/evidence/
GET    /wages/{id}/declarations/{dec_id}/evidence/{ev_id}/file/
POST   /wages/{id}/declarations/{dec_id}/verify/  (admin)

# النزاعات
GET    /disputes/
GET    /disputes/{id}/
POST   /disputes/
GET    /disputes/{id}/events/
POST   /admin/disputes/{id}/assign/             (admin)
POST   /admin/disputes/{id}/resolve/            (admin)

# التمويل
GET    /funding/proposals/
POST   /funding/proposals/
GET    /funding/proposals/{id}/
GET    /funding/proposals/{id}/budget/
POST   /admin/funding/proposals/{id}/approve/   (admin)
POST   /admin/funding/proposals/{id}/reject/    (admin)
POST   /admin/funding/proposals/{id}/allocate/  (admin)

# الإشعارات
GET    /notifications/
PATCH  /notifications/{id}/read/
POST   /notifications/read-all/

# الإدارة والتقارير
GET    /admin/accounts/
PATCH  /admin/accounts/{id}/verify/
PATCH  /admin/accounts/{id}/suspend/
GET    /admin/audit-log/
GET    /admin/reports/pilot-summary/
GET    /admin/reports/pilot-summary/export/     (CSV)

# النظام
GET    /health/                                 → {"status":"ok","db":true}
GET    /schema/                                 (dev فقط)
GET    /schema/swagger-ui/                      (dev فقط)
```

### رموز HTTP

| Code | المعنى |
|------|--------|
| 200 | نجاح |
| 201 | تم الإنشاء |
| 400 | خطأ في التحقق من المدخلات |
| 401 | غير مُصادق أو انتهى التوكن |
| 403 | مصادق لكن غير مُخوَّل |
| 404 | غير موجود (أو رُفض الوصول لأسباب الملكية) |
| 409 | تعارض (مثل طلب مكرر) |
| 429 | تجاوز حد الطلبات |
| 503 | قاعدة البيانات غير متصلة (يظهر فقط في `/health/`) |

---

## 8. المصادقة والصلاحيات

### JWT

```python
ACCESS_TOKEN_LIFETIME  = 15 دقيقة
REFRESH_TOKEN_LIFETIME = 7 أيام
ROTATE_REFRESH_TOKENS  = True   # كل تحديث يُنتج توكن جديد
BLACKLIST_AFTER_ROTATION = True  # التوكن القديم يُحظر فوراً
UPDATE_LAST_LOGIN      = True
AUTH_HEADER_TYPES      = ("Bearer",)
```

### طبقات الصلاحيات (بالترتيب)

1. **المصادقة** — توكن صالح مطلوب على كل endpoint
2. **صلاحية الدور** — `IsWorker` / `IsBusiness` / `IsAdminRole` / `IsVerifiedBusiness`
3. **ملكية الكائن** — المنشأة لا ترى وظائف غيرها، العامل لا يرى بيانات غيره
4. **القرارات الخادمية** — كل القرارات المالية وانتقالات الحالة على الخادم، واجهة التطبيق تزييني فقط

### التحقق من المنشأة (Business Verification)

```
التسجيل → PENDING (لا تستطيع نشر وظائف)
         ↓ Admin يُحقق
      APPROVED (تستطيع نشر وظائف وإعلان الدفع)
         ↓ عند المشكلة
      SUSPENDED
```

---

## 9. دورة حياة الوظيفة والأجور

### المسار الكامل

```
1. منشأة تُنشئ وظيفة (DRAFT)
2. Admin يُحقق المنشأة → APPROVED
3. منشأة تنشر الوظيفة (PUBLISHED)
4. عامل يتقدم (JobApplication: PENDING)
5. منشأة تقبل العامل (OFFERED) → Assignment يُنشأ تلقائياً
6. عامل يقبل التكليف → terms_snapshot يُحفظ (لا يتغير)
7. تسجيل الحضور + تأكيد الإنجاز
8. WageObligation يُنشأ (مرة واحدة فقط)
9. منشأة تُعلن الدفع (PaymentDeclaration: SUBMITTED)
   ├── نقداً: عامل يُقرّ → VERIFIED_PAID
   └── تحويل بنكي: Admin يُراجع → VERIFIED_PAID أو REJECTED
10. عند الرفض: منشأة تُعيد الإعلان → Admin يرى SUBMITTED الجديد
```

### حماية التزامن (Race Condition)

```python
@transaction.atomic
def post(self, request, pk):
    obligation = WageObligation.objects.select_for_update().get(pk=pk, ...)
    declaration = obligation.declarations.select_for_update().get(pk=dec_id)
```

مُطبَّق على `DeclarePaymentView`، `AcknowledgePaymentView`، `AdminVerifyPaymentView`.

### منطق `selectReviewableDeclaration()`

```kotlin
// Backend يُرجع الإعلانات بترتيب -declared_at (الأحدث أولاً)
// بعد رفض إعلان وإعادة الإعلان، تكون القائمة: [SUBMITTED, REJECTED]
// الدالة تختار الأول بحالة SUBMITTED بغض النظر عن الموضع
fun selectReviewableDeclaration(
    declarations: List<PaymentDeclarationResponse>
): PaymentDeclarationResponse? =
    declarations.firstOrNull { it.status == "SUBMITTED" }
```

---

## 10. الإعدادات والبيئات

### قواعد البيانات

| البيئة | اسم قاعدة البيانات |
|--------|-------------------|
| Development | `yawmiyati_dev` |
| Test | قاعدة بيانات مؤقتة (معزولة) |
| Production | `wathb_prod` (Render) |

### المنطقة الزمنية واللغة

```python
LANGUAGE_CODE = "ar"
TIME_ZONE = "Asia/Gaza"
USE_TZ = True
```

### تحميل الملفات

```python
ALLOWED_UPLOAD_CONTENT_TYPES = ["image/jpeg", "image/png", "application/pdf"]
MAX_UPLOAD_SIZE_BYTES = 5 * 1024 * 1024  # 5 MB
# التحقق بقراءة magic bytes (python-magic)، لا بالـ HTTP header
```

---

## 11. الاختبارات

### Django — 144 اختبار

| التطبيق | ملف الاختبار |
|---------|-------------|
| accounts | `accounts/tests/test_auth.py` |
| assignments | `assignments/tests/test_assignments.py` |
| audit | `audit/tests/test_audit.py` |
| disputes | `disputes/tests/test_disputes.py` |
| funding | `funding/tests/test_funding.py` |
| jobs | `jobs/tests/test_jobs.py` |
| wages | `wages/tests/test_wages.py` |
| e2e | `yawmiyati/tests/test_e2e.py` |

```bash
# تشغيل الاختبارات
python manage.py test --settings=yawmiyati.settings.test --verbosity=0 --keepdb
# النتيجة: Ran 144 tests in ~2s — OK
```

### Android JVM — 30 اختبار

| مجموعة الاختبارات | عدد | المحتوى |
|------------------|-----|---------|
| `WagesViewModelTest` | 18 | WagesViewModel، WorkerWagesViewModel، AdminWagesViewModel، session expiry (5 اختبارات) |
| `AuthRepositoryTest` | 7 | JWT storage، refresh، logout |
| `JobsViewModelTest` | 5 | تحميل الوظائف، الفلترة، الأخطاء |

```bash
# تشغيل اختبارات Android
./gradlew :app:testDebugUnitTest
# النتيجة: BUILD SUCCESSFUL — 30 tests, 0 failures, 0 errors
```

### بناء الـ APK

```bash
./gradlew assembleDebug
# النتيجة: app/build/outputs/apk/debug/app-debug.apk (19 MB)
```

---

## 12. النشر — Render

### الخدمات على Render

| الخدمة | النوع | الاسم |
|--------|-------|-------|
| Django API | Web Service | `wathb-api` |
| PostgreSQL | Database | `wathb-db` |

### إعدادات Web Service

```
Root Directory: backend
Build Command:  pip install -r requirements.txt
Start Command:  gunicorn yawmiyati.wsgi:application --workers 3 --bind 0.0.0.0:$PORT --timeout 30
```

### Health Endpoint

```
GET https://wathb-api.onrender.com/api/v1/health/
→ {"status":"ok","db":true}   HTTP 200   ✓ طبيعي
→ {"status":"degraded","db":false}  HTTP 503   ✗ قاعدة البيانات غير متصلة
```

عند حدوث 503، تظهر في Render Logs:
```
DB health-check failed [OperationalError]: <رسالة psycopg2 التفصيلية>
```

### تشخيص أخطاء قاعدة البيانات

| الرسالة في اللوج | السبب | الحل |
|-----------------|-------|------|
| `could not connect to server` | `DB_HOST` خاطئ | تحقق من hostname في Render dashboard |
| `password authentication failed` | `DB_PASSWORD` أو `DB_USER` خاطئ | أعد نسخ القيم من wathb-db |
| `database "..." does not exist` | `DB_NAME` خاطئ | تحقق من اسم قاعدة البيانات |
| `no pg_hba.conf entry` | SSL مطلوب | تأكد أن `DB_SSLMODE=require` مضبوط |
| `SSL SYSCALL error` | تعارض SSL | جرّب `DB_SSLMODE=prefer` |

---

## 13. الأمان

### تخزين التوكنات (Android)

- `EncryptedSharedPreferences` مع AES256-GCM
- لا يُكتب أي توكن في اللوجات
- انتهاء الصلاحية → `sessionExpired = true` → توجيه لتسجيل الدخول

### كلمات المرور

- تخزين: PBKDF2 + SHA256 + salt (Django المدمج)
- التحقق: 4 محقِّقات Django (التشابه، الحد الأدنى، القائمة السوداء، رقمي فقط)

### الحقول المالية

- المبالغ **لا تُضبط من قِبَل العميل** — تُحسب أو تُتحقق منها الخادم
- كل الأرقام عدد صحيح في الوحدات الفرعية (لا عمليات float للأموال)
- الحالات تتحقق من الانتقالات المسموحة فقط

### البيانات الحساسة

- رقم الهوية الوطنية: مخزون كـ SHA-256 فقط (لا الرقم الأصلي)
- ملفات الأدلة: مخزونة خارج المسار العام، محمية بالمصادقة
- لا يُسجَّل أي secret أو password أو توكن في اللوجات
- `.env` مستثنى من Git (`.gitignore`)

### CORS

- Development: مسموح للـ emulator
- Production: `CORS_ALLOWED_ORIGINS` من env var فقط، لا wildcard

### Rate Limiting

- Login: 10 محاولات/دقيقة/IP
- Token refresh: محدود بشكل مماثل
- التسجيل: محدود per IP

---

## 14. النسخ الاحتياطي والاستعادة

### النسخ اليومي (Cron 2:00 ص)

```bash
PGPASSWORD="$DB_PASSWORD" pg_dump \
  --host="$DB_HOST" --port="$DB_PORT" \
  --username="$DB_USER" --format=custom --compress=9 \
  --file="/var/backups/wathb/wathb_$(date +%Y%m%dT%H%M%S).dump" "$DB_NAME"
# + tar للـ media/ (ملفات الأدلة)
```

- الاحتفاظ بـ 7 نسخ يومية + 4 أسبوعية
- نقل النسخ خارج الخادم (S3 / Backblaze B2 / USB)

### الاستعادة

```bash
sudo systemctl stop wathb-gunicorn           # إيقاف الخادم أولاً
pg_restore --dbname="${DB_NAME}_restore" "$BACKUP_FILE"
# تحقق من البيانات → أعد تشغيل الخادم
```

### أهداف الـ Pilot

| المعيار | الهدف |
|---------|-------|
| RPO (نقطة الاسترداد) | 24 ساعة |
| RTO (وقت الاسترداد) | 4 ساعات |

---

## 15. حالة المشروع وسجل التغييرات

### سجل Commits

| Hash | الرسالة |
|------|---------|
| `b8155cd` | fix: add gunicorn==23.0.0 to requirements for Render deployment |
| `7b41eff` | feat: complete Wathb brand integration, security hardening, and pilot readiness |
| `79cf924` | chore: initialize Yawmiyati repository |

### ما تم إنجازه

- [x] Backend Django كامل — 10 تطبيقات، 144 اختبار
- [x] Android App — 13 شاشة، 30 اختبار JVM
- [x] هوية وَثْب البصرية الكاملة (الألوان، الخط، الشعار، الأيقونة)
- [x] خط ثمانية Sans بـ 5 أوزان مدمج في التطبيق
- [x] إعدادات الإنتاج المشددة (HSTS، SSL، CORS، CSP)
- [x] `@transaction.atomic` + `select_for_update()` على جميع كتابات الأجور
- [x] إصلاح F.2: `selectReviewableDeclaration()` تعرض SUBMITTED لا REJECTED
- [x] Session expiry في جميع ViewModels مع توجيه تلقائي
- [x] `.env.example` كامل بدون أسرار
- [x] وثائق التشغيل والنسخ الاحتياطي
- [x] Render deployment: gunicorn، SSL، logging

### ما يحتاج قراراً

| البند | الوضع |
|-------|-------|
| نطاق الإنتاج (`api.yawmiyati.com` vs نطاق آخر) | **قرار مطلوب** |
| بناء Release APK بـ URL الإنتاج | **قرار مطلوب بعد النطاق** |
| اختبار استعادة النسخ الاحتياطي على خادم حقيقي | **موصى به قبل Pilot عام** |
| تحقق بصري على محاكي/جهاز حقيقي بعد `b8155cd` | **NOT VERIFIED** |
| إضافة تحقق بريد إلكتروني عند التسجيل | MEDIUM (مؤجل) |
| تقييد IP لـ Django admin / 2FA | MEDIUM (مؤجل) |
| مسح الملفات المرفوعة من البرامج الضارة | MEDIUM (مؤجل) |

### خارج نطاق MVP (متعمَّد)

- بوابة دفع إلكتروني
- رسائل SMS / Push notifications
- التحقق البيومتري
- تتبع GPS
- تطابق ذكاء اصطناعي
- تحويل العملات
- واجهة ويب للمتصفح

---

*آخر تحديث: 2026-10-11 · commit `b8155cd`*
