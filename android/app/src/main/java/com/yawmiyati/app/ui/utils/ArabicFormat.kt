package com.yawmiyati.app.ui.utils

private val WESTERN = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
private val ARABIC  = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun String.toArabicNumerals(): String = buildString {
    for (c in this@toArabicNumerals) {
        val i = WESTERN.indexOf(c)
        append(if (i >= 0) ARABIC[i] else c)
    }
}

fun Int.toArabic(): String = toString().toArabicNumerals()
fun Long.toArabic(): String = toString().toArabicNumerals()

private val MONTH_NAMES = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

/** Accepts "YYYY-MM-DD" or full ISO datetime — returns "١٦ أكتوبر ٢٠٢٦" */
fun formatArabicDate(isoDate: String): String {
    return try {
        val datePart = isoDate.take(10)
        val parts = datePart.split("-")
        val year  = parts[0].toInt()
        val month = parts[1].toInt()
        val day   = parts[2].toInt()
        "${day.toArabic()} ${MONTH_NAMES[month - 1]} ${year.toArabic()}"
    } catch (e: Exception) {
        isoDate
    }
}

/** "07:00:00" or "07:00" → "٠٧:٠٠" */
fun formatArabicTime(isoTime: String): String = isoTime.take(5).toArabicNumerals()

/** wage_amount (minor units) → "٢٥٠ ₪ / يوم" */
fun formatArabicWage(minorUnits: Long, currency: String, basis: String): String {
    val major = (minorUnits / 100).toArabic()
    val sym = when (currency) { "ILS" -> "₪"; "USD" -> "$"; else -> currency }
    val basisAr = when (basis) {
        "PER_DAY"  -> "/ يوم"
        "PER_HOUR" -> "/ ساعة"
        "PER_TASK" -> "/ مهمة"
        else       -> ""
    }
    return "$major $sym $basisAr"
}
