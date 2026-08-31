# Lunaa Adaptive AOD Brightness — Design

## Goal
Build a minimal Vector/LSPosed module for crDroid 12.11 (Android 16, device codename `lunaa`) that replaces the ROM's fixed 0.05 AOD brightness with adaptive brightness driven by the ambient light sensor, while remaining safe to disable and avoiding kernel-level hooks.

## Confirmed root cause
Runtime `dumpsys display` from the target device shows:

- `mDefaultDozeBrightness = 0.05`
- `mScreenBrightnessDozeConfig = 0.05`
- `mDozeBrightnessSensorValueToBrightness = null`
- `mAllowAutoBrightnessWhileDozing = false`
- the active strategy in doze is `DozeBrightnessStrategy`
- `DozeScreenBrightness` resets to `mDefaultDozeBrightness` and calls `DozeMachine.Service.setDozeScreenBrightness(...)`

Therefore the target build has no *usable configured* native adaptive AOD sensor path and falls back to a fixed 0.05 doze brightness. crDroid 16 can fall back to `AlwaysOnDisplayPolicy` brightness arrays when the DisplayManager mapping is null, but those arrays are only useful when a compatible doze brightness sensor is configured; the target runtime remains fixed at 0.05. The temporarily brighter UDFPS phase is a separate transient display/dimming path, not the desired AOD baseline.

## crDroid 16 source validation

Repository research against `crdroidandroid/android_frameworks_base` branch `16.0` validates the integration layer:

- `DozeScreenBrightness` is `@DozeScope`, stores `mContext`, owns the doze brightness sensor lifecycle, and writes through `DozeMachine.Service.setDozeScreenBrightness(float)`.
- `DozeModule` deliberately places `DozeScreenBrightness` before `DozeScreenState` in the `DozeMachine.Part[]`, so brightness work happens before the physical display-state transition.
- `DozeService` runs in the main SystemUI process; no `android`/System Framework scope is required.
- crDroid's Oplus/Realme `UdfpsHelper` has a separate GHBM/dim-layer path and explicitly notes that its brightness-to-alpha logic does not account well for Doze brightness below the normal minimum. This explains why the UDFPS-visible phase can look brighter without making UDFPS the correct place to fix the base AOD brightness.
- A public Android 16 `lunaa` device tree with the same physical display ID uses a much lower constant `config_screenBrightnessDozeFloat`, while the target runtime reports 0.05. This supports the maintainer's statement that the target build overrides/hardcodes the constant; changing that resource alone would still only produce another fixed value.

Because on-device Vector logs show hook installation but no constructor-attached controller, runtime integration must not depend exclusively on a constructor callback. `transitionTo(...)` and `resetBrightnessToDefault()` are guaranteed lifecycle methods on the existing `DozeScreenBrightness` instance, so v1.0.2 late-attaches from their `thisObject` and recovers the exact crDroid `mContext` field. The constructor hook remains an optional fast path.

## Architecture
Use a legacy Xposed-compatible APK so it remains compatible with Vector's stable legacy API regardless of libxposed API 100/101/102 changes. Scope only `com.android.systemui`.

The module hooks `com.android.systemui.doze.DozeScreenBrightness` and manages a small state object per instance:

1. Capture the `DozeScreenBrightness` instance from its runtime lifecycle. The constructor hook is an optional fast path; `transitionTo(...)`/`resetBrightnessToDefault()` late-attach if needed by reading the exact crDroid `mContext` field from `thisObject`.
2. Use the physical display state as the primary lifecycle signal: adaptive control is active while the built-in display is `Display.STATE_DOZE` or `Display.STATE_DOZE_SUSPEND`. `transitionTo(oldState, newState)` is still hooked for an immediate state refresh. While the display is ON, sample the current display brightness every 2 seconds so the initial AOD target has a reliable recent pre-sleep value even if the Doze transition itself occurs after the panel has already dimmed/off.
3. Register a `SensorEventListener` for `Sensor.TYPE_LIGHT` while the display is ON or in DOZE/DOZE_SUSPEND. During ON it only remembers the latest lux so AOD can start with the correct pre-sleep ambient value; during DOZE it also drives adaptive brightness updates. Prefer the non-wakeup variant to avoid waking the CPU solely for lux updates, fall back to the default light sensor, and unregister for OFF and on `FINISH`.
4. Convert lux to an AOD brightness target through a conservative piecewise-linear curve.
5. Debounce and apply hysteresis so sensor noise does not cause visible flicker.
6. Call the existing `mDozeService.setDozeScreenBrightness(float)` via reflection, using the ROM's own service path rather than patching display drivers.
7. Hook `resetBrightnessToDefault()` after execution and re-apply the adaptive target when currently in an AOD state. This prevents the ROM's fixed `0.05` reset from winning.
8. Preserve a last-known lux/target in memory and use a safe fallback if the sensor has not produced a reading yet.

## Brightness policy
Default curve (normalized brightness):

| Lux | Target |
|---:|---:|
| 0 | 0.020 |
| 2 | 0.025 |
| 10 | 0.040 |
| 50 | 0.065 |
| 200 | 0.100 |
| 500 | 0.140 |
| 1000 | 0.180 |
| 5000 | 0.280 |
| 20000+ | 0.350 |

Piecewise-linear interpolation is used between points. Output is clamped to `[0.015, 0.35]`.

Default fallback before a valid sensor reading is `0.07`. A remembered normal-screen brightness is scaled by `0.90`, capped at `0.18`, and never allowed to seed AOD below the same `0.07` safe floor. Once a valid lux reading arrives, the full adaptive curve (including values below 0.07 in darkness) takes over.

Hysteresis: ignore updates smaller than both 10 lux and 8% relative lux change, unless 2 seconds have elapsed. Brightness writes are additionally suppressed when the normalized target changes by less than `0.005`.

## Safety boundaries
- Scope: `com.android.systemui` only.
- No `system_server`, kernel, KPatch, vendor HAL, or display-driver hooks in v1.
- No OverlayFS mutation in v1. OverlayFS is reserved as a separate fallback if SystemUI-level re-application is proven insufficient.
- All reflection/hook failures are caught and logged; module must fail open to stock AOD behavior rather than crash SystemUI.
- Sensor listener is unregistered on leaving AOD states and on `FINISH`.
- Brightness is capped at 0.35 to limit OLED burn-in/power risk.

## Diagnostics
Use log tag `LunaaAOD` and log only state changes, sensor registration, lux bucket changes, target changes, hook failures, and applied brightness. No high-frequency raw sensor spam.

Expected diagnostic example:

`state=DOZE_AOD lux=487.0 target=0.138 systemDefault=0.050 applied=0.138`

## Test strategy
Pure JVM tests cover:
- interpolation at every curve boundary
- interpolation between boundaries
- min/max clamping
- fallback behavior
- hysteresis/debounce decisions
- display-state classification (`STATE_DOZE`/`STATE_DOZE_SUSPEND` true; ON/OFF false)
- initial AOD target derived conservatively from the last screen brightness before doze

Android/Xposed integration is kept thin and manually verified with:
- APK build succeeds
- APK contains `assets/xposed_init`
- scope metadata restricts to SystemUI where supported
- logcat confirms module load and hook installation
- on-device `dumpsys display` changes from fixed `0.05` to the module-selected target during stable AOD
- UDFPS disappearance does not force target back to `0.05`
- covering/uncovering ALS changes AOD target without rapid flicker
- disabling module + reboot restores stock 0.05 behavior

## Non-goals for v1
- Settings UI
- user-editable curve
- persistence across reboot
- kernel/KPatch hooks
- systemless resource overlays
- brightness animation beyond the platform's existing display ramp
