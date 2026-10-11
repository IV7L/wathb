# Wathb Design Tokens

Design tokens are the single source of truth for all visual values.
Android Kotlin definitions are in `android/app/src/main/java/com/yawmiyati/app/ui/theme/`.

---

## Color Tokens

### Kotlin (Color.kt)

```kotlin
// Brand primitives
val WathbDeepTeal    = Color(0xFF006D6B)
val WathbDeepTealDk  = Color(0xFF004F4E)
val WathbTeal        = Color(0xFF0EA5A3)
val WathbTealLight   = Color(0xFFB2E8E7)
val WathbGold        = Color(0xFFF4B432)
val WathbGoldDark    = Color(0xFFC48A14)
val WathbGoldLight   = Color(0xFFFDEAB0)
val WathbWarmCream   = Color(0xFFFEF3D7)

// Neutral scale
val Neutral900 = Color(0xFF1C1B1F)
val Neutral600 = Color(0xFF49454F)
val Neutral200 = Color(0xFFE6E0E9)
val Neutral50  = Color(0xFFFEFAF5)

// Error
val ErrorRed          = Color(0xFFB3261E)
val ErrorRedContainer = Color(0xFFF9DEDC)
```

### CSS Custom Properties (web reference)
```css
:root {
  --color-deep-teal:    #006D6B;
  --color-deep-teal-dk: #004F4E;
  --color-teal:         #0EA5A3;
  --color-teal-light:   #B2E8E7;
  --color-gold:         #F4B432;
  --color-gold-dark:    #C48A14;
  --color-gold-light:   #FDEAB0;
  --color-warm-cream:   #FEF3D7;
  --color-neutral-900:  #1C1B1F;
  --color-neutral-600:  #49454F;
  --color-neutral-200:  #E6E0E9;
  --color-neutral-50:   #FEFAF5;
  --color-error:        #B3261E;
}
```

---

## Spacing Tokens

### Kotlin (Spacing.kt)

```kotlin
object WathbSpacing {
    val xs =  4.dp
    val sm =  8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

Usage pattern: `Spacer(Modifier.height(WathbSpacing.md))`

---

## Shape Tokens

### Kotlin (Shape.kt)

```kotlin
val WathbShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
```

General rules:
- Cards: `medium` (12dp)
- Buttons: `small` (8dp)
- Chips / badges: `extraSmall` (4dp)
- Dialogs: `large` (16dp)
- Bottom sheets: `extraLarge` top corners (24dp)

---

## Typography Tokens

Scale (all weights pulled from system/default font, Arabic-compatible):

| Role | Size | Weight | Line Height | Use |
|------|------|--------|-------------|-----|
| displayLarge | 28sp | Bold | 36sp | Brand name on login |
| headlineMedium | 22sp | SemiBold | 30sp | Screen titles |
| titleLarge | 18sp | SemiBold | 26sp | Card headers |
| titleMedium | 16sp | Medium | 24sp | List item titles |
| bodyLarge | 16sp | Normal | 24sp | Main body text |
| bodyMedium | 14sp | Normal | 20sp | Secondary body |
| labelLarge | 14sp | Medium | 20sp | Button text |
| labelMedium | 12sp | Normal | 16sp | Field labels |
| labelSmall | 11sp | Medium | 16sp | Status badges |

**Arabic note:** `letterSpacing = 0.sp` on all styles (Arabic script does not use letter-spacing).

---

## Elevation Tokens

| Level | dp | Use |
|-------|----|-----|
| 0 | 0dp | Flat surfaces (backgrounds) |
| 1 | 1dp | Default card |
| 2 | 2dp | Raised card (standard) |
| 4 | 4dp | Floating elements |
| 8 | 8dp | Dialogs |
| 12 | 12dp | Bottom sheets |

---

## Icon Sizes

| Use | Size |
|-----|------|
| Navigation icons | 24dp |
| Action bar icons | 24dp |
| Status badges | N/A (text only) |
| FAB icon | 24dp |
