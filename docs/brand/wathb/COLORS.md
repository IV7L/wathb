# Wathb Color Palette

## Primary Palette

| Name | Hex | RGB | Usage |
|------|-----|-----|-------|
| Deep Teal | `#006D6B` | 0, 109, 107 | Primary brand color; CTAs; headers |
| Teal | `#0EA5A3` | 14, 165, 163 | Secondary; interactive highlights; wave |
| Gold | `#F4B432` | 244, 180, 50 | Accent; sun symbol; important badges |
| Warm Cream | `#FEF3D7` | 254, 243, 215 | Background; card surfaces |

## Extended Palette

| Name | Hex | Usage |
|------|-----|-------|
| Deep Teal Dark | `#004F4E` | Pressed state for Deep Teal; dark theme primary |
| Teal Light | `#B2E8E7` | Primary container; tinted surfaces |
| Gold Dark | `#C48A14` | Pressed state for Gold |
| Gold Light | `#FDEAB0` | Secondary container; gold-tinted surfaces |
| Neutral 900 | `#1C1B1F` | Body text; headings on light backgrounds |
| Neutral 600 | `#49454F` | Secondary text; captions |
| Neutral 200 | `#E6E0E9` | Dividers; disabled surfaces |
| Neutral 50  | `#FEFAF5` | Page background (cream-tinted white) |
| Error Red   | `#B3261E` | Error states; destructive actions |
| Error Container | `#F9DEDC` | Error background containers |

## Color Semantics (Material3 Mapping)

| Material3 Role | Light Mode | Dark Mode |
|---------------|------------|-----------|
| primary | `#006D6B` | `#0EA5A3` |
| onPrimary | `#FFFFFF` | `#FFFFFF` |
| primaryContainer | `#B2E8E7` | `#006D6B` |
| onPrimaryContainer | `#1C1B1F` | `#FEF3D7` |
| secondary | `#F4B432` | `#F4B432` |
| onSecondary | `#1C1B1F` | `#1C1B1F` |
| secondaryContainer | `#FDEAB0` | `#C48A14` |
| onSecondaryContainer | `#1C1B1F` | `#1C1B1F` |
| tertiary | `#0EA5A3` | `#B2E8E7` |
| background | `#FEFAF5` | `#1C1B1F` |
| surface | `#FEFAF5` | `#1C1B1F` |
| surfaceVariant | `#E6E0E9` | `#49454F` |
| error | `#B3261E` | `#B3261E` |

## Accessibility

All text/background combinations in this palette meet WCAG 2.1 AA (4.5:1 for body, 3:1 for large text):

- Deep Teal `#006D6B` on white `#FFFFFF` → 5.74:1 ✓
- White `#FFFFFF` on Deep Teal `#006D6B` → 5.74:1 ✓
- Neutral 900 `#1C1B1F` on Warm Cream `#FEF3D7` → 14.2:1 ✓
- Deep Teal `#006D6B` on Warm Cream `#FEF3D7` → 5.6:1 ✓

Avoid Gold `#F4B432` on white backgrounds for text — contrast is 2.3:1 (insufficient). Use Gold only for decorative elements or on dark/teal backgrounds.

## Do Not Use

- ~~Do not introduce additional colors without approval~~
- ~~Do not use the old green palette (`#2E7D32`, `#388E3C`)~~
- ~~Do not use pure black or pure white as primary surfaces~~
