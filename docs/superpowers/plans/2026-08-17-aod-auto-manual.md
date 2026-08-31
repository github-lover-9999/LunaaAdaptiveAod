# Lunaa Adaptive AOD v1.3 Automatic + Manual Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v1.3.0 with stock fallback, Automatic AOD brightness with presets/custom curve/floor/stabilization/smoothing, and Manual fixed-brightness mode.

**Architecture:** Keep the proven SystemUI-only hook and normal `TYPE_LIGHT` source. Extend the immutable settings snapshot with mode/floor/manual values, keep output policy pure in `BrightnessCurve`/`AdaptiveAodEngine`, and isolate time-based output interpolation in a pure `BrightnessSmoother` consumed by `AdaptiveAodController`. The native launcher UI edits the same snapshot and conditionally shows Automatic or Manual controls.

**Tech Stack:** Java 17, Android SDK 36 platform widgets, Vector/legacy Xposed API stubs, JUnit 4 source/unit tests.

## Global Constraints
- Vector scope only `com.android.systemui`.
- No System Framework / `android` scope.
- No UDFPS hook.
- No constructor hook, `mContext`, `ActivityThread`, or new main-thread `Handler` fallback.
- Use `android.sensor.light`; do not depend on `qti.sensor.lux_aod`.
- Master OFF makes no `setDozeScreenBrightness()` calls.
- Explicit Save; SystemUI reloads settings at next AOD activation.
- Automatic multiplier remains 50–300% in 5% steps.
- All normalized brightness values remain 0.010–1.000.

---

### Task 1: Extend the settings model and migration

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/AodMode.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsSnapshot.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsDefaults.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java`
- Modify: `app/src/main/java/dev/lunaa/aod/XposedSettingsReader.java`
- Test: `app/src/test/java/dev/lunaa/aod/AodSettingsTest.java`

**Interfaces:**
- Produces `AodMode { AUTOMATIC, MANUAL }` with `persistedValue()` and `fromPersistedValue(int)`.
- `AodSettingsSnapshot` exposes `getMode()`, `getMinimumAutoBrightness()`, `getManualBrightness()`.
- Codec keys: `mode`, `minimum_auto_brightness`, `manual_brightness`.

- [ ] **Step 1: Write failing settings tests** for default mode/floor/manual value, v1.2 migration defaults, round-trip persistence, and range rejection.
- [ ] **Step 2: Run the pure JVM suite and verify RED** because the new APIs/keys do not exist.
- [ ] **Step 3: Implement `AodMode`, snapshot fields, defaults, codec read/write, and richer SystemUI settings log.**
- [ ] **Step 4: Run all pure JVM tests and verify GREEN.**
- [ ] **Step 5: Commit** `feat: add auto and manual AOD settings model`.

### Task 2: Add Automatic floor, Manual policy, and entry guard

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/BrightnessCurve.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AdaptiveAodEngine.java`
- Test: `app/src/test/java/dev/lunaa/aod/BrightnessCurveTest.java`
- Test: `app/src/test/java/dev/lunaa/aod/AdaptiveAodEngineTest.java`

**Interfaces:**
- `BrightnessCurve.targetForLux()` applies multiplier then `minimumAutoBrightness`.
- `AdaptiveAodEngine.prepareAmbientEntry()` and `setAmbientActive()` return manual fixed brightness in Manual mode.
- `onLux()` always remembers valid lux; it returns no output in Manual/Disabled modes.
- Entry-dark guard: 1200 ms, two downward samples required before accepting a drop during guard.

- [ ] **Step 1: Write failing tests** for floor after multiplier, Manual fixed output, Disabled NaN behavior, pre-doze remembered lux, and first-dark-sample suppression.
- [ ] **Step 2: Run targeted tests and verify RED.**
- [ ] **Step 3: Implement minimal curve/engine policy.**
- [ ] **Step 4: Run full JVM suite and verify GREEN.**
- [ ] **Step 5: Commit** `feat: add manual policy and automatic entry guard`.

### Task 3: Add deterministic asymmetric brightness smoothing

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/BrightnessSmoother.java`
- Test: `app/src/test/java/dev/lunaa/aod/BrightnessSmootherTest.java`

**Interfaces:**
- `snap(long nowMs, float value)` sets current/start/target immediately.
- `retarget(long nowMs, float target)` starts from interpolated current value.
- `valueAt(long nowMs)` returns clamped interpolated output.
- `isRunning(long nowMs)` reports whether another frame is needed.
- Constants: 400 ms brighten, 1400 ms darken, 100 ms frame interval.

- [ ] **Step 1: Write failing tests** for brighten duration, darken duration, retarget-from-current, finish state, and clamping.
- [ ] **Step 2: Run targeted test and verify RED.**
- [ ] **Step 3: Implement `BrightnessSmoother`.**
- [ ] **Step 4: Run full JVM suite and verify GREEN.**
- [ ] **Step 5: Commit** `feat: add asymmetric AOD brightness smoothing`.

### Task 4: Integrate immediate entry application and smoothing into SystemUI controller

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java`
- Modify: `app/src/main/java/dev/lunaa/aod/SystemUiHooks.java` only if needed to preserve call order
- Modify: `app/src/test/java/dev/lunaa/aod/StartupDipWiringTest.java`
- Modify: `app/src/test/java/dev/lunaa/aod/SystemUiHooksWiringTest.java`

**Interfaces:**
- `onDozeTransition(Object)` immediately writes the prepared entry target when enabled.
- Lux-driven target changes call a smoothing request rather than direct bridge writes.
- `reapplyAfterReset()` writes the current smoothed value/target immediately, without creating a controller.
- Sensor registration: screen ON tracks lux when module is enabled; ambient tracks lux only for Automatic mode.

- [ ] **Step 1: Write failing source-wiring assertions** for immediate prepared-target apply, smoothing runner, and no direct lux bridge write.
- [ ] **Step 2: Run source tests and verify RED.**
- [ ] **Step 3: Integrate `BrightnessSmoother` using the existing SystemUI `Handler`; add a single 100 ms runnable and cancellation on destroy/non-ambient.**
- [ ] **Step 4: Run all JVM/source tests plus Xposed ABI check and verify GREEN.**
- [ ] **Step 5: Commit** `fix: stabilize AOD entry and brightness transitions`.

### Task 5: Redesign native settings UI for Automatic / Manual

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/AodPreset.java`
- Create: `app/src/main/java/dev/lunaa/aod/SettingsUiTheme.java`
- Modify: `app/src/main/java/dev/lunaa/aod/SettingsActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/test/java/dev/lunaa/aod/SettingsUiSourceTest.java`
- Test: `app/src/test/java/dev/lunaa/aod/AodPresetTest.java`

**Interfaces:**
- `AodPreset.detect(snapshot)` returns DIM/BALANCED/BRIGHT/CUSTOM based on curve points only.
- Activity owns two draft panels, toggled by `AodMode`.
- Automatic panel: presets, multiplier, minimum floor, 9 point curve.
- Manual panel: 0.010–1.000 slider + exact normalized input.
- Bottom Reset/Save remains visible outside the scroll body.

- [ ] **Step 1: Write failing preset and UI source tests** for mode buttons, minimum slider, manual slider/input, conditional panels, Custom label, and fixed bottom actions.
- [ ] **Step 2: Run targeted tests and verify RED.**
- [ ] **Step 3: Implement `AodPreset`, AMOLED theme helpers, and the new programmatic layout without AndroidX or new dependencies.**
- [ ] **Step 4: Run full JVM/source suite and verify GREEN.**
- [ ] **Step 5: Commit** `feat: redesign settings for automatic and manual modes`.

### Task 6: Update release metadata, docs, QA, and Windows build output

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `tools/build-windows.ps1`
- Modify: `README.md`
- Modify: `QA.md`
- Modify: `app/src/test/java/dev/lunaa/aod/ReleaseMetadataTest.java`

**Interfaces:**
- versionCode `7`, versionName `1.3.0`.
- Windows output `dist\LunaaAdaptiveAod-v1.3.0-debug.apk`.
- QA includes stock fallback, Auto, Manual, floor, smoothing/entry and existing UDFPS regression checks.

- [ ] **Step 1: Update release tests first and verify RED.**
- [ ] **Step 2: Update metadata/docs/build script.**
- [ ] **Step 3: Run full JVM/source suite and Xposed ABI check and verify GREEN.**
- [ ] **Step 4: Run `git diff --check`.**
- [ ] **Step 5: Commit** `docs: prepare v1.3.0 release`.

### Task 7: Final verification and standalone source package

**Files:**
- No production changes unless verification exposes a bug.
- Create deliverable outside git: `/mnt/data/LunaaAdaptiveAod-v1.3.0-source.zip`.

**Interfaces:**
- Standalone archive excludes `.git`, `.worktrees`, `.gradle`, `.tools`, build outputs and local properties.

- [ ] **Step 1: Run the complete custom local JVM/source suite and require zero failures.**
- [ ] **Step 2: Run `python3 tools/test_xposed_stub_contract.py`.**
- [ ] **Step 3: Run source invariants for SystemUI-only scope and forbidden hooks/fields.**
- [ ] **Step 4: Run `git status`, `git diff --check`, inspect final log.**
- [ ] **Step 5: Package source ZIP and compute SHA-256.**
- [ ] **Step 6: Report that a real Android APK build still requires the Windows build script unless an Android SDK/Gradle environment is available locally.**
