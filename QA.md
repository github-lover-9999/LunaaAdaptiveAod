# Lunaa Adaptive AOD — QA & Verification Matrix

## Test Suite Summary
- Total Automated Unit Tests: **171 tests** (100% passing)
- Key Test Classes:
  - `AppUpdaterTest`: Version comparison, JSON parsing, update notification rules.
  - `BrightnessCurveTest`: Perceptual brightness calculation across 0–20,000 lux.
  - `AodPresetTest`: DIM (20%), BALANCED (50%), BRIGHT (100%) floor verification.
  - `LunaaDevicePolicyTest`: Multi-ROM and Oplus board probe validation.
  - `RootBridgeSenderPolicyTest`: SystemUI UID authorization security verification.
  - `ExtraBrightnessWiringTest`: Hardware latching and UDFPS watchdog testing.
  - `DozeRampTest`, `InstantDozeRampTest`, `SystemServerHooksWiringTest`: the ROM's AOD brightness ramp and the System Framework hook that skips it.
  - `BatterySaverAodTest`, `BatterySaverAodWiringTest`: the Keep AOD on in Battery Saver option.

## Logcat Diagnostic Checklist
When analyzing AOD behavior on a physical target:

```bash
# Filter essential Lunaa AOD and SystemUI logs:
adb logcat -d -b all -v threadtime | grep -E "LunaaAOD|LunaaAODRoot|DisplayPowerController|DozeBridge|notify_fppress|dsi_cmd"
```

Expected output after boot, with **System Framework** checked in the module's scope:
1. `LunaaAOD: doze ramp hook installed in system_server` (also in the LSPosed/Vector module log).
2. On the first AOD entry: `LunaaAOD: doze brightness applied without the display ramp (was rate=0.06 target=...)`. Without this line the ROM still ramps each AOD brightness change for up to 3 s: check the scope and reboot.
3. `LunaaAOD: Battery Saver AOD hook installed in system_server`.

Expected output with **Keep AOD on in Battery Saver** on:
1. Turn Battery Saver on: `LunaaAOD: Battery Saver keeps AOD on (disable_aod answered false)` (once per boot), and locking the phone shows AOD.
2. `adb shell settings get global low_power` still prints `1`: Battery Saver stays on, only its AOD limit is gone.
3. With the option off, Battery Saver turns AOD off again from the next time it turns on.

Expected output on screen lock (AOD entry):
1. `LunaaAOD: controller attached source=runtime-fields`
2. `LunaaAOD: aod brightness scale=... hbmTransitionPoint=... sliderMax=... displayMax=...` — the brightest normal AOD brightness. Manual levels and automatic targets are shares of it (Bright = 100% = the scale). On lunaa it should be 0.735 (`hbmTransitionPoint=0.73527`): AOD shows nothing brighter than the HBM transition point, which is why Balanced and Bright used to look the same.
3. `LunaaAOD: reason=screen-off-animation target=... applied` — target is the brightness the screen showed before locking (the dimmed level after a timeout), so the 4 s screen-off animation neither drops nor flashes.
4. About 4 s later: `LunaaAOD: doze brightness reclaimed from stock displayState=...`, `LunaaAOD: reason=enter-doze target=... applied` and `LunaaAOD: reason=enter-doze from=... systemRampMs=... animatorScale=...` — the module sets the AOD level in one step. With the System Framework hook the panel shows it at once; without it the ROM ramps the change for `systemRampMs` (up to 3 s while animations are on, `animatorScale` > 0).
5. With Extra Bright: `LunaaAOD: extraBright enable-pending delayMs=...` — the HBM step comes 250 ms after entering AOD. Without the System Framework hook, `LunaaAOD: extraBright waits for the system AOD brightness ramp delayMs=...` follows and the step lands right after the ramp (`systemRampMs` + 150 ms). After a pulse (fingerprint touch or notification) Extra Bright waits 4 s, so a second fingerprint touch is not disturbed. In Automatic mode, bright light the sensor reported before AOD counts towards the 0.6 s Extra Bright dwell.
6. `DisplayPowerController: BrightnessEvent: brt=...`
7. `LunaaAODRoot: FP logical reset executed via app root process`

Expected output in Automatic mode when a hand covers the sensors on AOD and is removed:
1. `LunaaAOD: doze state=DOZE_AOD_PAUSING` — the proximity sensor paused AOD; a dark reading from just before the pause is discarded.
2. `LunaaAOD: lux=0 ambient=true ignored: sensors covered` — readings of the covered light sensor do not change the AOD brightness.
3. `LunaaAOD: doze state=DOZE_AOD` and `LunaaAOD: reason=prepare-aod target=...` — the same level as before the cover, without waiting for a new light reading. The light sensor reports only changes, so its last reading stays valid while it is registered.

Expected output in Automatic mode when the light changes on AOD:
1. `LunaaAOD: reason=lux target=... applied` — each light change lands in one step. Changes too small to see (under 0.01, about 1%, in the perceived brightness scale) are skipped until they add up.

Expected output on fingerprint unlock from AOD (touch → pulse → unlock):
1. `LunaaAOD: reason=handback-DOZE_REQUEST_PULSE target=...` — target is at most the normal screen brightness, so the unlock screen never flashes to the AOD level.
2. `LunaaAOD: extraBright yielded notify_fppress to stock fingerprint release=false` — with Extra Bright latched and already rearmed, the module leaves `notify_fppress` to the stock UDFPS path.
3. `LunaaAOD: extraBright ambient-session-end reason=ambient-exit ... reset=false dimHeldMs=...` — with Extra Bright on, the dim layer stays at the chosen strength through the touch. The kernel sizes its fingerprint dim layer from the backlight, so the layer stays until the backlight is down at the handback level: `dimHeldMs=300` with the System Framework hook, up to about 3 s without it (the ROM ramp). Without the held layer the touch showed the screen at full HBM. The layer lies below the UDFPS overlay, so it never covers the sensor spot.
4. `LunaaAOD: extraBright pulse dim released reason=doze-finish` (unlocked), `reason=aod-resumed` (back to AOD after a failed touch) or `reason=timeout`.
5. No `LunaaAODRoot: FP logical reset executed via app root process` between the touch and the unlock. A touch while the HBM edge is still being written gives `LunaaAODRoot: FP logical reset skipped: the module holds no press` instead of a write that would cancel the real finger press.
6. After a failed touch that returns to AOD: `LunaaAOD: doze brightness reclaimed from stock displayState=4`.