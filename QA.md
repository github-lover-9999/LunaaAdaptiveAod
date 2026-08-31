# Lunaa Adaptive AOD — QA & Verification Matrix

## Test Suite Summary
- Total Automated Unit Tests: **105 tests** (100% passing)
- Key Test Classes:
  - `AppUpdaterTest`: Version comparison, JSON parsing, update notification rules.
  - `BrightnessCurveTest`: Perceptual brightness calculation across 0–20,000 lux.
  - `AodPresetTest`: DIM (20%), BALANCED (50%), BRIGHT (100%) floor verification.
  - `LunaaDevicePolicyTest`: Multi-ROM and Oplus board probe validation.
  - `RootBridgeSenderPolicyTest`: SystemUI UID authorization security verification.
  - `ExtraBrightnessWiringTest`: Hardware latching and UDFPS watchdog testing.

## Logcat Diagnostic Checklist
When analyzing AOD behavior on a physical target:

```bash
# Filter essential Lunaa AOD and SystemUI logs:
adb logcat -d -b all -v threadtime | grep -E "LunaaAOD|LunaaAODRoot|DisplayPowerController|DozeBridge|notify_fppress|dsi_cmd"
```

Expected output on screen lock (AOD entry):
1. `LunaaAOD: controller attached source=runtime-fields`
2. `LunaaAOD: reason=enter-doze target=... applied`
3. `DisplayPowerController: BrightnessEvent: brt=...`
4. `LunaaAODRoot: FP logical reset executed via app root process`