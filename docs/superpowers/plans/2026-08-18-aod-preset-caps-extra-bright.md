# AOD Preset Caps and Extra Bright Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v1.4.0 with simple preset maximum sliders, daytime-readable Balanced behavior, sensor-gated Bright Extra Bright, and a simplified Manual slider.

**Architecture:** Persist the selected preset and three caps in the existing XSharedPreferences transport. Clamp automatic curve output by the active preset cap. Keep vendor AOD-HBM isolated behind a fail-safe Oplus controller and a pure state-machine policy so normal brightness remains functional if vendor control fails.

**Tech Stack:** Android 16 Java 17, legacy Xposed/Vector, platform widgets, JUnit 4 source tests.

**Spec:** `docs/superpowers/specs/2026-08-18-aod-preset-caps-extra-bright-design.md`

## Global Constraints

- Scope only `com.android.systemui`.
- Keep normal `android.sensor.light`.
- No constructor hooks, `mContext`, `ActivityThread`, qti `lux_aod`, or UDFPS class hook.
- Dim cap 0–40 default 35; Balanced 40–80 default 80; Bright 70–100 default 100.
- Extra Bright only for Automatic + Bright + cap >=90 + strong daylight.
- Manual never forces Extra Bright.
- Fail safe to normal AOD when vendor control is unavailable.

---

### Task 1: Preset caps and persistence

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/AodPreset.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsDefaults.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsSnapshot.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java`
- Test: `app/src/test/java/dev/lunaa/aod/AodPresetTest.java`
- Test: `app/src/test/java/dev/lunaa/aod/AodSettingsTest.java`

**Interfaces:**
- Produces preset cap getters, persisted preset id, per-preset cap fields, and v1.3.x migration defaults.

- [ ] Write tests for ranges/defaults, cap validation, persistence round-trip, and legacy migration.
- [ ] Run tests and confirm the new assertions fail.
- [ ] Implement the minimal snapshot/default/codec changes.
- [ ] Run all JVM source tests and confirm green.
- [ ] Commit.

### Task 2: Automatic curves and Extra Bright policy

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/BrightnessCurve.java`
- Create: `app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java`
- Test: `app/src/test/java/dev/lunaa/aod/BrightnessCurveTest.java`
- Create: `app/src/test/java/dev/lunaa/aod/ExtraBrightnessPolicyTest.java`

**Interfaces:**
- Produces capped automatic targets and `ExtraBrightnessPolicy.update(long, AodSettingsSnapshot, float, boolean)`.

- [ ] Write failing curve-cap and policy hysteresis/dwell tests.
- [ ] Run tests and confirm failure for missing behavior.
- [ ] Implement the calibrated curves/cap clamp and policy state machine.
- [ ] Run all JVM source tests and confirm green.
- [ ] Commit.

### Task 3: Vendor Extra Bright controller and SystemUI wiring

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/OplusExtraBrightnessController.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java`
- Test: `app/src/test/java/dev/lunaa/aod/SystemUiHooksWiringTest.java`
- Create: `app/src/test/java/dev/lunaa/aod/ExtraBrightnessWiringTest.java`

**Interfaces:**
- Consumes `ExtraBrightnessPolicy` and selected settings.
- Produces fail-safe vendor enable/disable/reapply operations.

- [ ] Write source-wiring tests proving Bright-only policy, Oplus path, fail-safe fallback, off-on-exit, and reapply-after-reset.
- [ ] Confirm tests fail before production code changes.
- [ ] Implement controller and integrate with ambient/lux lifecycle.
- [ ] Run compile and JVM tests.
- [ ] Commit.

### Task 4: Simplified settings UI

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/SettingsActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/dev/lunaa/aod/SettingsUiSourceTest.java`

**Interfaces:**
- Reads/writes selected preset, three caps, mode, master enabled, and manual brightness.

- [ ] Replace source tests with assertions for the new preset-cap UI and absence of advanced curve controls.
- [ ] Confirm tests fail.
- [ ] Implement the simplified Automatic/Manual UI while preserving edge-to-edge insets and >=48dp controls.
- [ ] Run compile and JVM tests.
- [ ] Commit.

### Task 5: Release metadata, docs, and package verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `tools/build-windows.ps1`
- Modify: `README.md`
- Modify: `QA.md`
- Modify: `app/src/test/java/dev/lunaa/aod/ReleaseMetadataTest.java`

**Interfaces:**
- Produces v1.4.0 source and build instructions.

- [ ] Write/update metadata tests for versionCode 9 / versionName 1.4.0.
- [ ] Confirm metadata test fails before bump.
- [ ] Update metadata/docs/build output name.
- [ ] Run strict Java compile, all JVM source tests, Xposed ABI contract, XML parse, policy greps, and `git diff --check`.
- [ ] Create a clean source ZIP excluding `.git`, build products, `.tools`, and `dist`.
- [ ] Verify ZIP integrity and SHA-256.
