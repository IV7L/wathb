# Wathb Logo Concept

## Concept: The Leap

The Wathb logo depicts a human figure in mid-leap — the moment of maximum commitment, when both feet have left the ground. This captures the platform's promise: we help workers and businesses make the leap from uncertainty to opportunity.

## Symbol Anatomy

```
        ☀ (Gold Sun)
       ●        ← Head
      /|\       ← Arms (one raised forward, one back)
      / \
     /   \      ← Torso at forward angle
    /     \
   ←       →   ← Legs (front leg extending, back leg trailing)
 ~~~~~~~~~~~~~~~  ← Teal Wave (Gaza's coast; the sea of opportunity)
```

### Element Meanings

| Element | Visual | Meaning |
|---------|--------|---------|
| Leaping figure | White silhouette | The worker/business in motion; universal humanity |
| Gold sun | Circle upper-right | Hope; opportunity ahead; the brightness of a new workday |
| Teal wave | Flowing arc at base | Gaza's Mediterranean coast; the flow of commerce |
| Deep teal background | #006D6B | Depth, trustworthiness, stability |

## Color Variants

### Full Color (preferred)
- Background: Deep Teal `#006D6B`
- Figure: White `#FFFFFF`
- Sun: Gold `#F4B432`
- Wave: Teal `#0EA5A3`

### Light on Dark
Same as full color. Use when placing on teal backgrounds.

### Monochrome (single color)
- All elements in Deep Teal `#006D6B` on white, or
- All elements in White `#FFFFFF` on Deep Teal

### Reversed (for dark mode or dark backgrounds)
- Background: `#004F4E`
- Figure: White
- Sun: Gold
- Wave: `#0EA5A3`

## Symbol Construction

The symbol is constructed on a 100×100 grid:

- **Safe zone:** 10px margin on all sides → effective canvas 80×80
- **Figure head:** Circle at (38, 28), radius 6
- **Torso:** Diagonal stroke from (38, 34) to (43, 51), weight 5
- **Right arm:** Stroke from (40, 38) to (56, 28), weight 4 (raised, forward)
- **Left arm:** Stroke from (40, 38) to (26, 44), weight 4 (trailing back)
- **Right leg:** Stroke from (43, 51) to (59, 57), weight 5 (extending forward)
- **Left leg:** Stroke from (43, 51) to (32, 63), weight 5 (trailing back)
- **Sun:** Circle at (76, 24), radius 11, filled Gold
- **Wave:** Bezier arc M0,75 C16,67 32,82 48,74 C64,66 76,80 100,73 → fills to bottom

## Android Launcher Icon

The launcher icon is an adaptive icon:
- **Background layer:** Solid `#006D6B` (see `ic_launcher_background.xml`)
- **Foreground layer:** Wathb symbol at 108dp viewport, safe zone [18,18]–[90,90]
  (see `ic_launcher_foreground.xml`)

The foreground positions:
- Gold sun: center (78, 28), radius 12
- Figure: scaled to fit within [32,26]–[74,72]
- Wave: spans [18,76]–[90,90]

## File Inventory

| File | Format | Use |
|------|--------|-----|
| `assets/wathb-symbol.svg` | SVG | Full-color symbol (docs, web) |
| `assets/wathb-wordmark-ar.svg` | SVG | Arabic wordmark only |
| `assets/wathb-wordmark-en.svg` | SVG | English wordmark only |
| `assets/wathb-lockup.svg` | SVG | Full horizontal lockup |
| `assets/wathb-monochrome.svg` | SVG | Single-color version |
| `assets/wathb-icon-preview.svg` | SVG | App icon visual preview |
| `android/.../ic_launcher_foreground.xml` | AVD | Android adaptive icon foreground |
| `android/.../ic_launcher_background.xml` | XML | Android adaptive icon background |
