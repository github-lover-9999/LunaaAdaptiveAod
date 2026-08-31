# Auto Calibration and Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Make Automatic Brightness meaningfully distinct, make Auto Extra Bright reachable, and safely support compatible lunaa/RMX336* ROM builds without weakening fail-closed HBM safeguards.

**Architecture:** Separate pure automatic-brightness calibration from pure device/capability policy. Keep the proven root/HBM controller unchanged except for a compatibility preflight in the companion app. SystemUI hook discovery gains conservative exact-or-unique-type field resolution, while vendor HBM remains gated by rooted sysfs probes.

**Tech Stack:** Android 16 Java 17, legacy Xposed/Vector, JUnit 4, Python source-policy tests, PowerShell QA tooling.

**Spec:** `docs/superpowers/specs/2026-08-19-auto-and-compatibility-design.md`

## Global Constraints

- Preserve the proven Oplus `notify_fppress` 0->1 root bridge and 250 ms AOD handoff.
- Extra Bright is available only for Manual Bright or Automatic BRIGHT / Daylight.
- Unknown or ambiguous runtime contracts fail closed.
- ROM name alone never enables vendor HBM.
- No direct `su` execution inside SystemUI.

---

### Task 1: Automatic brightness envelopes

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/AutomaticBrightnessProfile.java`
- Modify: `app/src/main/java/dev/lunaa/aod/BrightnessCurve.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodPreset.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java`
- Test: `app/src/test/java/dev/lunaa/aod/AutomaticBrightnessProfileTest.java`
- Test: `app/src/test/java/dev/lunaa/aod/BrightnessCurveTest.java`
- Test: `app/src/test/java/dev/lunaa/aod/AodSettingsTest.java`

**Interfaces:**
- Produces: `AutomaticBrightnessProfile.targetFor(float lux, AodSettingsSnapshot settings)` returning a 0.01..1.0 brightness request.

- [x] Write tests proving DIM/BALANCED/BRIGHT are strongly ordered at 50, 500, 1000, and 5000 lux and proving cap changes scale the response before the ceiling.
- [x] Run the focused tests and confirm they fail against v1.5.6 behavior.
- [x] Implement the normalized response envelope and new cap defaults 25/55/100 with floors 3/8/18.
- [x] Add exact-old-default migration for 35/80/100 while preserving custom caps.
- [x] Run focused tests and the full unit suite.

### Task 2: Reachable Automatic Extra Bright

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java`
- Modify: `app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java`
- Modify: `app/src/main/java/dev/lunaa/aod/SettingsActivity.java`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/dev/lunaa/aod/ExtraBrightnessPolicyTest.java`

**Interfaces:**
- `ExtraBrightnessPolicy.ENABLE_LUX = 1500f`
- `ExtraBrightnessPolicy.DISABLE_LUX = 700f`
- `ExtraBrightnessPolicy.ENABLE_DWELL_MS = 600L`
- `ExtraBrightnessPolicy.DISABLE_DWELL_MS = 1500L`

- [x] Write failing tests for the new enable/disable thresholds, invalid-lux fail-closed behavior, and BRIGHT-only eligibility.
- [x] Verify RED.
- [x] Implement the new policy and increase recent-lux validity to 15 seconds.
- [x] Update Settings preview/ambient copy to match the actual trigger.
- [x] Verify GREEN and full unit suite.

### Task 3: Device compatibility policy

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/LunaaDevicePolicy.java`
- Create: `app/src/main/java/dev/lunaa/aod/HbmCapabilityProbe.java`
- Modify: `app/src/main/java/dev/lunaa/aod/RootHbmBridgeReceiver.java`
- Test: `app/src/test/java/dev/lunaa/aod/LunaaDevicePolicyTest.java`
- Test: `app/src/test/java/dev/lunaa/aod/HbmCapabilityProbeSourceTest.java`

**Interfaces:**
- `LunaaDevicePolicy.isSupportedIdentity(String device, String product, String model, String manufacturer)`
- `HbmCapabilityProbe.probeViaRoot()` returning immutable capability result with reason.

- [x] Write failing identity tests for lunaa+RMX3360, lunaa+RMX3363, guarded RMX336* variants, unrelated Realme, and unrelated devices.
- [x] Write source test requiring rooted existence checks for notify_fppress, dimlayer_hbm, and panel brightness before any write.
- [x] Verify RED.
- [x] Implement policy and rooted capability probe.
- [x] Gate every root HBM command in `RootHbmBridgeReceiver` before `runRootWrite`.
- [x] Verify GREEN.

### Task 4: Conservative SystemUI hook compatibility

**Files:**
- Modify: `app/src/main/java/dev/lunaa/aod/SystemUiHooks.java`
- Test: `app/src/test/java/dev/lunaa/aod/SystemUiHooksWiringTest.java`
- Create: `app/src/test/java/dev/lunaa/aod/RuntimeFieldResolverTest.java`
- Create: `app/src/main/java/dev/lunaa/aod/RuntimeFieldResolver.java`

**Interfaces:**
- `RuntimeFieldResolver.readExactOrUniqueAssignable(Object instance, String preferredName, Class<?> requiredType)` returns the resolved field value or null on absence/ambiguity.

- [x] Write failing pure-Java resolver tests for preferred field, unique typed fallback, ambiguous typed fallback, and missing field.
- [x] Verify RED.
- [x] Implement conservative resolver and use it for SensorManager/DisplayManager/Handler and optional Context.
- [x] Keep transitionTo mandatory; make resetBrightnessToDefault hook optional with a degraded-mode log.
- [x] Verify GREEN and source wiring tests.

### Task 5: Diagnostics and release safety

**Files:**
- Modify: `tools/collect-aod-qa.ps1`
- Create: `tools/test_v160_compatibility_source.py`
- Modify: `tools/test_pre_release_audit.py`
- Modify: `README.md`
- Modify: `QA.md`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/dev/lunaa/aod/ReleaseMetadataTest.java`

**Interfaces:**
- QA collector emits device/ROM properties, exact settings XML, focused Lunaa logs, capability nodes, display state, and clean AOD kernel delta.

- [x] Write source-policy checks for no SystemUI `su`, root capability-before-write, fail-closed ambiguous hook fields, and preserved 250 ms handoff.
- [x] Verify RED for version/diagnostic requirements.
- [x] Update QA collector, docs, and version to 1.6.0.
- [x] Run strict production compile, full unit suite, all Python/source checks, standalone safety mains, XML parsing, forbidden-regression grep, and ZIP integrity.
