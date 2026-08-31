# Lunaa AOD v1.6.4 Source Recovery Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the approved v1.6.4 behavior onto the last working v1.6.2 Java source line without reusing the corrupted FIXED7+ dex lineage.

**Architecture:** Keep the existing v1.6.2 controller/HBM architecture and preference codec. Add only the missing state for manual Extra Bright, lock automatic profiles to fixed presets, remove the automatic cap slider from UI, and add delayed post-reset brightness reapply. Compile source to class files, rebuild dex with D8, package over the known-good APK resources/manifest, and sign with the project keystore.

**Tech Stack:** Java 17, Android platform APIs, Xposed/LSPosed stubs, JUnit 4, D8/R8 8.13.x, APK Signature Scheme v2/v3.

**Spec:** Project conversation + current confirmed recovery baseline.

## Global Constraints

- Manual Balanced default = 50%.
- Manual Bright = 100% normal AOD brightness without HBM unless Manual Extra Bright is enabled.
- Manual Extra Bright is a separate toggle visible only for Manual Bright; strength Low/Medium/Max, default Low.
- Automatic mode has no percentage/cap slider.
- Automatic Balanced uses the former Bright 100% normal-brightness envelope.
- Automatic Bright Daylight adds automatic Extra Bright/HBM with Low/Medium/Max strength.
- Automatic HBM enable threshold = >=1500 lux for 600 ms; disable threshold = <700 lux for 1500 ms.
- HBM control path remains notify_fppress only; no direct SystemUI su and no UDFPS/qti hook.
- Bottom actions remain inset-safe; checked switches use bright yellow accent fill.
- After stock reset, brightness is reapplied immediately and again after 400 ms.
- Build from working Java source only; never use FIXED7+ class/dex artifacts as source of truth.

---

### Task 1: Lock behavior in regression tests

**Files:**
- Modify: `app/src/test/java/dev/lunaa/aod/AodSettingsTest.java`
- Modify: `app/src/test/java/dev/lunaa/aod/ExtraBrightnessPolicyTest.java`
- Modify: `app/src/test/java/dev/lunaa/aod/SettingsUiSourceTest.java`
- Modify: `app/src/test/java/dev/lunaa/aod/StartupDipWiringTest.java`

- [ ] Add tests for 50% Manual Balanced, Low Extra Bright default, manual toggle persistence/default-off, fixed automatic profiles, no automatic cap SeekBar, bright-yellow checked track, and delayed 400 ms reset reapply.
- [ ] Run focused tests and record RED failures.

### Task 2: Settings model and migration

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsSnapshot.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsDefaults.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java`

- [ ] Add `manualExtraBrightEnabled` state while preserving legacy constructors.
- [ ] Change Manual Balanced default to 50% and Extra Bright selected level to Low.
- [ ] Add migration key `manual_extra_bright_enabled`, default false when absent.
- [ ] Lock automatic caps to preset defaults on read/write because the UI cap control is removed.
- [ ] Run focused model tests to GREEN.

### Task 3: Extra Bright policy

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java`

- [ ] Manual mode requests HBM only for Manual Bright + explicit manual Extra Bright enabled.
- [ ] Automatic Bright Daylight retains 1500/700 lux hysteresis independent of the manual toggle.
- [ ] Run policy tests to GREEN.

### Task 4: Settings UI

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/SettingsActivity.java`
- Modify: `app/src/main/java/dev/lunaa/aod/SettingsUiTheme.java`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] Remove automatic maximum-brightness/cap SeekBar and percent copy.
- [ ] Present fixed auto presets: Dim, Balanced, Bright Daylight; show Extra Bright strength for Bright Daylight.
- [ ] Add separate Manual Extra Bright Switch shown only for Manual Bright.
- [ ] Keep Low/Medium/Max strength shared and default Low.
- [ ] Make checked switch track bright yellow, not brown.
- [ ] Preserve bottom insets and Save/Reset behavior.
- [ ] Run UI source tests to GREEN.

### Task 5: Late reset protection

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java`
- Modify: `app/src/main/java/dev/lunaa/aod/SystemUiHooks.java` only if required by tests.

- [ ] Add delayed 400 ms `udfps-recovery` reapply after stock `resetBrightnessToDefault()` without creating a new controller.
- [ ] Cancel/ignore delayed work after controller destruction.
- [ ] Run startup/reset wiring tests to GREEN.

### Task 6: Build and release verification

**Files:**
- Modify: `app/build.gradle.kts` version metadata only.
- Create: final signed APK under `/mnt/data/`.

- [ ] Run all unit/source tests.
- [ ] Compile all changed Java source against a verified Android 16 API classpath and existing Xposed stubs; compare framework descriptors against the known-good v1.6.2 classes.
- [ ] Build dex with D8 from Java class outputs, not from corrupted dex.
- [ ] Package with unchanged known-good resources/manifest except version metadata if required.
- [ ] Sign with `LunaaAod-fixed.keystore` and verify v2/v3 plus matching certificate.
- [ ] Verify ZIP integrity, resources.arsc STORED + 4-byte alignment, `SettingsActivity` present, and no invalid framework descriptors.
