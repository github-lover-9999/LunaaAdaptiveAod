# Lunaa Adaptive AOD v1.3 Design System

## Product context
A device-specific native Android settings app for a Vector/legacy-Xposed SystemUI module on `lunaa`. The screen must make AOD brightness behavior understandable without exposing unnecessary Android internals. It supports a master off state, Automatic mode with presets/curve tuning, and Manual fixed-brightness mode.

## Visual direction
AMOLED-first utility UI: restrained, high-contrast, compact, native-feeling, and clearly grouped. Keep the warm-yellow accent from the existing app. No gradients, glassmorphism, decorative imagery, or third-party Material dependency.

## Tokens
- background `#0B0B0C`
- surface `#161618`
- elevated `#1C1C1F`
- text `#F5F5F7`
- secondary `#A6A6AD`
- tertiary `#77777F`
- accent `#F3C85B`
- divider `#2A2A2E`
- warning `#E8A75B`
- card radius 16dp; control radius 12dp; chip radius 999dp
- horizontal padding 20dp; card padding 16dp; section gap 16dp
- system sans-serif only

## Screen structure
1. Header: app name + concise description.
2. Master card: Enabled switch, stock-fallback explanation, save/revision status.
3. Mode card: segmented Automatic / Manual control.
4. Live status card: Automatic shows ambient lux and effective AOD target; Manual hides lux and shows only the fixed target.
5. Automatic content (visible only in Automatic): preset selector as the primary control, then a collapsed Advanced settings disclosure containing Brightness adjustment, Minimum brightness, Advanced curve, warning.
6. Manual content (visible only in Manual): Fixed AOD brightness slider + exact normalized numeric input.
7. Persistent bottom action bar: Reset and Save.

## Interaction rules
- Master OFF means no brightness writes by the module; crDroid stock behavior is restored.
- Automatic uses `android.sensor.light`, curve interpolation, multiplier, minimum floor, entry stabilization, and asymmetric smoothing.
- Manual ignores ambient light for output and applies one fixed value.
- Dim / Balanced / Bright load curve values. Editing any curve point makes the effective preset Custom.
- Settings are saved explicitly and consumed on the next AOD activation.
- Values > 0.50 remain allowed but show OLED/power warning.

## Copy rules
Use short plain-English labels. Explain normalized brightness once, not repeatedly. Avoid jargon such as `DozeScreenBrightness`, Xposed or HAL in primary UI copy; such details belong in README/QA.

## Android safe areas
- targetSdk 36: API 30+ uses edge-to-edge with platform WindowInsets.
- Top content clears status bar/display cutout.
- Sticky actions clear navigation/gesture and IME bottom insets.
- Every interactive target is at least 48dp.
