package com.yawmiyati.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary              = WathbDeepTeal,
    onPrimary            = OnDark,
    primaryContainer     = WathbTealLight,
    onPrimaryContainer   = Neutral900,
    secondary            = WathbGold,
    onSecondary          = OnLight,
    secondaryContainer   = WathbGoldLight,
    onSecondaryContainer = Neutral900,
    tertiary             = WathbTeal,
    onTertiary           = OnDark,
    tertiaryContainer    = WathbTealLight,
    onTertiaryContainer  = Neutral900,
    background           = Neutral50,
    onBackground         = Neutral900,
    surface              = Neutral50,
    onSurface            = Neutral900,
    surfaceVariant       = Neutral200,
    onSurfaceVariant     = Neutral600,
    error                = ErrorRed,
    onError              = OnDark,
    errorContainer       = ErrorRedContainer,
    onErrorContainer     = Neutral900,
)

private val DarkColorScheme = darkColorScheme(
    primary              = WathbTeal,
    onPrimary            = OnDark,
    primaryContainer     = WathbDeepTeal,
    onPrimaryContainer   = WathbWarmCream,
    secondary            = WathbGold,
    onSecondary          = OnLight,
    secondaryContainer   = WathbGoldDark,
    onSecondaryContainer = OnLight,
    tertiary             = WathbTealLight,
    onTertiary           = OnLight,
    background           = Neutral900,
    onBackground         = Neutral50,
    surface              = Neutral900,
    onSurface            = Neutral200,
    surfaceVariant       = Neutral600,
    onSurfaceVariant     = Neutral200,
    error                = ErrorRed,
    onError              = OnDark,
    errorContainer       = ErrorRedContainer,
    onErrorContainer     = Neutral900,
)

@Composable
fun YawmiyatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = WathbShapes,
        content     = content
    )
}
