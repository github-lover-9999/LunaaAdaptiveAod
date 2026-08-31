# Lunaa Adaptive AOD v1.3 — Automatic + Manual Design

## Goal
Turn the proven v1.2 adaptive AOD path into a stable two-mode controller: **Automatic** with presets/custom curve and **Manual** with one fixed brightness value, while preserving a true stock fallback when the module master switch is off.

## Non-negotiable constraints
- Vector/legacy-Xposed scope remains **only `com.android.systemui`**.
- No `android` / System Framework scope.
- No UDFPS hook.
- No constructor hook and no `mContext` dependency.
- Keep the proven `android.sensor.light` path; do not depend on undocumented `qti.sensor.lux_aod`.
- Master OFF performs **no brightness writes** and returns behavior to crDroid's stock fixed doze request (`0.05` on the target build).
- Settings are explicit-save and reload on the next AOD activation; no polling loop.

## Settings model
Persist one validated snapshot containing:
- `enabled: boolean`
- `mode: AUTOMATIC | MANUAL`
- `multiplierPercent: 50..300`, step 5, Automatic only
- `minimumAutoBrightness: 0.010..1.000`
- `manualBrightness: 0.010..1.000`
- 9 strictly-increasing lux points and 9 brightness values (`0.010..1.000`)
- `revision >= 0`

Malformed or partially missing new fields must migrate safely from v1.2: enabled, multiplier and curve retain existing values; mode defaults Automatic; minimum floor defaults `0.020`; manual brightness defaults `0.150`.

## Output logic
### Automatic
`curve(lux) -> multiplier -> minimum floor -> requested target`

The minimum floor is applied after the multiplier and before output clamping. Output always remains `0.010..1.000`.

### Manual
`manualBrightness -> requested target`

Ambient lux remains visible in the app as information but never changes Manual output.

### Disabled
Return `NaN` from all module target decisions. The controller must not call `setDozeScreenBrightness()` while disabled.

## AOD entry and transition behavior
- Continue collecting normal `TYPE_LIGHT` values while the display is ON so a recent lux value exists before DOZE.
- On an Automatic AOD transition, calculate the entry target from recent lux when available; otherwise use the existing screen-brightness fallback.
- Apply the prepared target immediately at transition entry, reapply after stock `resetBrightnessToDefault()`, and apply once more when the physical display reaches DOZE.
- Manual mode prepares/applies the fixed value immediately.
- Do not accept one anomalously dark first post-DOZE sensor event as an instant large drop. During the first `1200 ms`, downward changes require either two consistent dark samples or expiry of the guard period. Brightening is never delayed by this guard.
- After the entry guard, sensor changes pass through the existing update gate.

## Brightness smoothing
Brightness changes after entry are animated by the existing SystemUI `Handler`, not by sleeping or blocking the sensor callback.
- Brightening duration: `400 ms`.
- Darkening duration: `1400 ms`.
- Frame interval: `100 ms`.
- A new target replaces the in-flight transition from the currently applied value.
- Reapply after stock reset writes the current transition value/target immediately and does not restart the animation from stock `0.05`.
- Manual mode entry can be applied immediately; later settings only take effect on a new AOD activation.

## UI design
Native Android widgets only, no new dependency. AMOLED-first dark UI with explicit cards and a warm-yellow accent.

### Layout
1. **Header** — `Lunaa Adaptive AOD` + short purpose line.
2. **Master card** — module switch and status. OFF copy explicitly states that stock crDroid AOD brightness is used.
3. **Mode card** — segmented `Automatic` / `Manual` selector.
4. **Live card** — ambient lux + effective target.
5. **Automatic panel**
   - `Dim / Balanced / Bright` preset buttons; edited curve is labelled `Custom`.
   - Overall brightness slider `50–300%`.
   - Minimum automatic brightness slider `0.010–0.300` for practical tuning, plus exact value display. The persisted validator still allows up to `1.000` for forward compatibility.
   - 9 editable Lux -> Brightness points with the existing plain-language hints.
6. **Manual panel**
   - Fixed brightness slider `0.010–1.000` mapped to `1–100%` UI progress.
   - Exact normalized numeric input beside/below it.
7. **Bottom action bar** — Reset and Save outside the scrolling content so actions remain reachable.

Automatic controls are hidden in Manual; Manual controls are hidden in Automatic. If the master switch is OFF, mode controls remain editable but output preview clearly says stock fallback.

## Presets
Keep the existing v1.2 Dim, Balanced and Bright curves unchanged so runtime comparisons remain meaningful. Presets replace only the 9 curve values; they do not overwrite the user's multiplier, minimum floor, mode or manual value.

## Reset behavior
Reset restores:
- enabled = true
- mode = Automatic
- Balanced curve
- multiplier = 100%
- minimum auto brightness = `0.020`
- manual brightness = `0.150`

Reset changes the form only; Save is still required.

## Logging
On settings revision change, SystemUI logs mode as well as multiplier, for example:
`LunaaAOD: settings revision=4 mode=AUTO multiplier=150% min=0.060 manual=0.150`

Runtime target logs keep the reason and target and may include source (`entry`, `lux`, `manual`, `reset`) without logging on every smoothing frame unless debug-worthy.

## Testing
- Pure JVM tests for settings migration/validation, Auto/Manual/Disabled target selection, floor behavior, entry guard and smoothing planner.
- Source-wiring tests assert SystemUI-only architecture, no `mContext`, no constructor hook, no UDFPS hook, and immediate prepared-target application.
- UI source tests assert Automatic/Manual controls, manual exact input, minimum floor, presets and hidden-mode sections.
- Existing 29 v1.2 tests remain passing or are updated only when product copy/version expectations intentionally change.
- Xposed stub ABI contract remains passing.
- Runtime QA must verify stock fallback, Auto adaptation, Manual fixed output, entry transition, UDFPS reset resilience and no SystemUI crash.
