# Lunaa Adaptive AOD Brightness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and verify a minimal legacy-Xposed APK for Vector that gives crDroid 12.11 `lunaa` adaptive AOD brightness instead of the ROM's fixed 0.05 doze brightness.

**Architecture:** Keep Android/Xposed integration thin and place all brightness math/state decisions in pure Java classes covered by JVM tests. Hook only `com.android.systemui.doze.DozeScreenBrightness` inside `com.android.systemui`; register a light sensor during always-on doze states and re-apply the computed target after ROM resets.

**Tech Stack:** Java 17, Android Gradle Plugin 8.13.2, Gradle 8.13, compileSdk 36, legacy Xposed API stubs as `compileOnly`, JUnit 4.

## Global Constraints

- Target ROM: crDroid 12.11 / Android 16 / `lunaa`.
- Target framework: Vector/JingMatrix with legacy Xposed compatibility.
- Hook scope: `com.android.systemui` only.
- No kernel, KPatch, system_server, vendor HAL, or OverlayFS mutation in v1.
- Fail open: reflection/hook errors must not crash SystemUI.
- AOD target range: 0.015 to 0.35.
- Fallback target before first valid lux: 0.07.
- Log tag: `LunaaAOD`.

---

### Task 1: Buildable module skeleton and policy tests

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/assets/xposed_init`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/java/dev/lunaa/aod/BrightnessCurve.java`
- Create: `app/src/main/java/dev/lunaa/aod/UpdateGate.java`
- Create: `app/src/test/java/dev/lunaa/aod/BrightnessCurveTest.java`
- Create: `app/src/test/java/dev/lunaa/aod/UpdateGateTest.java`
- Create: `xposed-stubs/src/main/java/...` minimal compile-only legacy Xposed signatures used by the module

**Interfaces:**
- Produces: `BrightnessCurve.targetForLux(float): float`
- Produces: `UpdateGate.shouldApply(long nowMs, float lux, float target): boolean`

- [ ] **Step 1: Write failing `BrightnessCurveTest`** covering exact knots, interpolation, negative lux fallback and high-lux cap.
- [ ] **Step 2: Run `./gradlew testDebugUnitTest` and verify RED** because `BrightnessCurve` is absent.
- [ ] **Step 3: Implement `BrightnessCurve`** with knots `{0,2,10,50,200,500,1000,5000,20000}` and targets `{.020,.025,.040,.065,.100,.140,.180,.280,.350}` plus clamp `[.015,.35]` and fallback `.07` for invalid readings.
- [ ] **Step 4: Run tests and verify curve tests GREEN.**
- [ ] **Step 5: Write failing `UpdateGateTest`** for first sample, tiny-change suppression, 8% relative threshold, 10-lux absolute threshold and 2-second forced refresh.
- [ ] **Step 6: Implement `UpdateGate`** storing last applied lux/target/time and updating only when thresholds are met.
- [ ] **Step 7: Run all unit tests and commit.**

### Task 2: Doze state model and safe reflective bridge

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/DisplayStatePolicy.java`
- Create: `app/src/main/java/dev/lunaa/aod/DozeBridge.java`
- Create: `app/src/test/java/dev/lunaa/aod/DisplayStatePolicyTest.java`

**Interfaces:**
- Produces: `DisplayStatePolicy.isAmbientState(int displayState): boolean`
- Produces: `DozeBridge.applyBrightness(Object dozeScreenBrightness, float target): boolean`

- [ ] **Step 1: Write failing state-policy test** asserting true for Android display states `DOZE` (3) and `DOZE_SUSPEND` (4), false for ON/OFF.
- [ ] **Step 2: Run test and verify RED.**
- [ ] **Step 3: Implement `DisplayStatePolicy`.**
- [ ] **Step 4: Implement `DozeBridge`** using Xposed reflection helpers to read `mDozeService` and invoke `setDozeScreenBrightness(float)`, catching every throwable and returning false on failure.
- [ ] **Step 5: Run tests and compile to verify no direct compile-time dependency on SystemUI internals.**
- [ ] **Step 6: Commit.**

### Task 3: Sensor lifecycle controller

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java`
- Create: `app/src/main/java/dev/lunaa/aod/Clock.java`
- Create: `app/src/test/java/dev/lunaa/aod/AdaptiveAodControllerTest.java`

**Interfaces:**
- Consumes: `BrightnessCurve`, `UpdateGate`, `DozeBridge`, `DisplayStatePolicy`
- Produces: `captureScreenBrightness()`, `refreshDisplayState()`, `reapplyAfterReset()`, `destroy()`
- Runtime detail: sample normal display brightness every 2 seconds while `STATE_ON`; observe non-wakeup-first `TYPE_LIGHT` during ON to remember pre-sleep lux and during DOZE/DOZE_SUSPEND to adapt brightness.

- [ ] **Step 1: Write failing controller tests** using a fake sink and clock for screen-brightness-derived initial target on entering DOZE, lux-to-target application, suppression of duplicate writes, and no writes after returning ON/OFF.
- [ ] **Step 2: Run tests and verify RED.**
- [ ] **Step 3: Implement controller state machine** with in-memory last screen brightness/lux/target, safe fallback 0.07, a 2-second normal-screen brightness sampler, display listener lifecycle, and idempotent non-wakeup-first light-sensor registration.
- [ ] **Step 4: Run tests and verify GREEN.**
- [ ] **Step 5: Commit.**

### Task 4: Vector/legacy Xposed SystemUI integration

**Files:**
- Create: `app/src/main/java/dev/lunaa/aod/LunaaAodModule.java`
- Create: `app/src/main/java/dev/lunaa/aod/SystemUiHooks.java`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/assets/xposed_init`

**Interfaces:**
- Entry: `LunaaAodModule implements IXposedHookLoadPackage`
- Scope guard: package name must equal `com.android.systemui`

- [ ] **Step 1: Add compile-only legacy Xposed stubs** sufficient for `IXposedHookLoadPackage`, `XC_MethodHook`, `XposedBridge`, `XposedHelpers`, `XC_LoadPackage.LoadPackageParam`.
- [ ] **Step 2: Hook all constructors of `DozeScreenBrightness`** as an optional fast path, while keeping controller identity per exact instance.
- [ ] **Step 3: Hook `transitionTo(State, State)` before/after execution**; late-attach from `param.thisObject` when the constructor callback did not attach, recovering `mContext` from the exact crDroid field. Before original capture current normal display brightness, after original refresh the physical display state. On `FINISH`, destroy listeners.
- [ ] **Step 4: Obtain `Context` from constructor arguments by selecting the first `android.content.Context` instance; create `DisplayManager` listener plus default `TYPE_LIGHT` listener, registering the light sensor only while the physical display reports DOZE/DOZE_SUSPEND.**
- [ ] **Step 5: Hook `resetBrightnessToDefault()` after execution**, also using late-attach as a fallback, and call `reapplyAfterReset()` only while adaptive state is active.
- [ ] **Step 6: Add bounded logging and failure guards** so any missing field/method disables the custom behavior for that instance instead of throwing into SystemUI.
- [ ] **Step 7: Build and inspect APK for `assets/xposed_init`, manifest Xposed metadata, and absence of bundled Xposed API implementation classes.**
- [ ] **Step 8: Commit.**

### Task 5: Static QA and release packaging

**Files:**
- Create: `README.md`
- Create: `QA.md`

**Interfaces:**
- Produces: installable debug/release APK and exact on-device verification commands.

- [ ] **Step 1: Run `./gradlew clean testDebugUnitTest assembleDebug`.**
- [ ] **Step 2: Run `aapt2 dump badging`/zip inspection** to verify package, SDK metadata and `assets/xposed_init`.
- [ ] **Step 3: Run `jadx` or `javap`/DEX inspection** to confirm no accidental direct linkage to `com.android.systemui.*` classes and no Xposed stubs packaged into the APK.
- [ ] **Step 4: Document Vector setup:** enable module, scope only System UI, reboot.
- [ ] **Step 5: Document runtime QA:** capture `logcat -s LunaaAOD:*`, reproduce UDFPS disappearance, then use `dumpsys display` to confirm `dozeScreenBrightness` is no longer fixed at `0.05` while AOD is active.
- [ ] **Step 6: Document rollback:** disable module in Vector and reboot; stock 0.05 behavior returns.
- [ ] **Step 7: Commit release docs and artifacts metadata.**
