package com.yawmiyati.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.yawmiyati.app.R

// Thamanyah Sans — primary typeface for Wathb (وَثْب).
// letterSpacing = 0.sp throughout: Arabic script does not use letter-spacing.
val ThamanyahSans = FontFamily(
    Font(R.font.thmanyahsans_light,   FontWeight.Light),
    Font(R.font.thmanyahsans_regular, FontWeight.Normal),
    Font(R.font.thmanyahsans_medium,  FontWeight.Medium),
    Font(R.font.thmanyahsans_bold,    FontWeight.Bold),
    Font(R.font.thmanyahsans_black,   FontWeight.Black),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Black,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Bold,
        fontSize      = 18.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Light,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = ThamanyahSans,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.sp,
    ),
)
