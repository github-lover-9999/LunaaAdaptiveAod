# Theme

## Compact token summary

The v1.2 UI relies on the device dark theme and platform widget styling rather than explicit tokens. The target v1.3 design introduces explicit AMOLED-first tokens while retaining the existing warm-yellow accent visible in the current app screenshot.

- Background: `#0B0B0C`
- Surface: `#161618`
- Elevated surface: `#1C1C1F`
- Primary text: `#F5F5F7`
- Secondary text: `#A6A6AD`
- Tertiary text: `#77777F`
- Accent: `#F3C85B`
- Divider: `#2A2A2E`
- Warning: `#E8A75B`
- Radius: 16dp cards, 12dp controls, 999dp chips
- Horizontal screen padding: 20dp
- Card padding: 16dp
- Section gap: 16dp
- Typography: system sans-serif; title 28sp semibold, section 19sp semibold, body 14sp, metric 17sp semibold

## Raw UI resources
```xml
<resources>
    <string name="app_name">Lunaa Adaptive AOD</string>
    <string name="app_description">Adaptive AOD brightness for crDroid Android 16 on lunaa</string>
    <string name="settings_intro">Tune adaptive AOD brightness without changing the SystemUI hook scope.</string>
    <string name="adaptive_aod">Adaptive AOD</string>
    <string name="apply_next_cycle">Changes apply on the next AOD activation</string>
    <string name="ambient_waiting">Ambient light: waiting for sensor…</string>
    <string name="preview_waiting">Calculated AOD brightness: waiting for sensor…</string>
    <string name="preview_invalid">Calculated AOD brightness: fix curve values</string>
    <string name="light_sensor_unavailable">Ambient light: TYPE_LIGHT sensor unavailable</string>
    <string name="overall_brightness">Overall brightness</string>
    <string name="multiplier_help">Scales the whole curve without changing its shape. 100% uses the curve values as entered.</string>
    <string name="preset_dim">Dim</string>
    <string name="preset_balanced">Balanced</string>
    <string name="preset_bright">Bright</string>
    <string name="advanced_curve">Advanced curve</string>
    <string name="curve_help">Edit Lux → Brightness points. Brightness is a normalized 0.010–1.000 panel request, not a linear perceived screen percentage.</string>
    <string name="lux_hint">Lux</string>
    <string name="brightness_hint">Brightness 0.010–1.000</string>
    <string name="save">Save</string>
    <string name="reset">Reset</string>
    <string name="reset_not_saved">Balanced defaults restored in the form. Tap Save to apply them.</string>
    <string name="preset_not_saved">Preset loaded into the form. Tap Save to apply it.</string>
    <string name="shared_prefs_unavailable">Vector/Xposed shared preferences are unavailable. Save is disabled; keep the module enabled only in SystemUI and verify framework support.</string>
    <string name="fix_curve_errors">Fix the highlighted curve values before saving.</string>
    <string name="save_failed">Settings could not be committed.</string>
    <string name="invalid_lux">Enter a finite Lux value ≥ 0.</string>
    <string name="lux_must_increase">Lux must be greater than the previous point.</string>
    <string name="invalid_brightness">Brightness must be between 0.010 and 1.000.</string>
    <string name="high_brightness_warning">High AOD brightness (&gt; 0.50) can increase power use and OLED wear.</string>
    <string name="empty"></string>
</resources>

```
