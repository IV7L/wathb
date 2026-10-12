import hashlib
import hmac
import re

_MOBILE_NINE = ("59", "56")  # 9-digit form (after stripping leading 0 from local)


def normalize_phone(raw: str) -> str:
    """
    Normalize a Palestinian mobile number to E.164 (+970XXXXXXXXX).

    Accepted input formats:
      0599xxxxxx   — local with leading zero
      +970599xxxxxx — E.164 with Gaza code
      00970599xxxxxx — ITU-T 00 prefix
      +972599xxxxxx  — Israeli SIM, same physical network

    Raises ValueError for invalid or non-Palestinian-mobile numbers.
    """
    s = raw.strip()

    if s.startswith("+972"):
        nine = s[4:]
    elif s.startswith("+970"):
        nine = s[4:]
    elif s.startswith("00970"):
        nine = s[5:]
    elif s.startswith("0"):
        nine = s[1:]
    else:
        raise ValueError("رقم الهاتف غير صالح.")

    if not re.fullmatch(r"\d{9}", nine):
        raise ValueError("رقم الهاتف يجب أن يتكون من 10 أرقام بعد صفر البداية.")

    if not any(nine.startswith(p) for p in _MOBILE_NINE):
        raise ValueError("يُقبل فقط أرقام الهاتف المحمول الفلسطيني (059، 056).")

    return "+970" + nine


def normalize_national_id(raw: str) -> str:
    """Strip whitespace/dashes and validate a 9-digit Palestinian national ID."""
    cleaned = re.sub(r"[\s\-]", "", raw.strip())
    if not re.fullmatch(r"\d{9}", cleaned):
        raise ValueError("رقم الهوية يجب أن يتكون من 9 أرقام.")
    return cleaned


def hash_national_id(raw: str) -> str:
    """Return HMAC-SHA256 hex digest of the normalized national ID."""
    from django.conf import settings
    key = getattr(settings, "NATIONAL_ID_HMAC_KEY", "")
    if not key:
        raise ValueError("NATIONAL_ID_HMAC_KEY غير مضبوط.")
    normalized = normalize_national_id(raw)
    return hmac.new(key.encode(), normalized.encode(), hashlib.sha256).hexdigest()
