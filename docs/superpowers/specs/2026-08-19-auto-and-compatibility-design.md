# AOD Control Auto Calibration and Compatibility Design

## Goal
Make Automatic Brightness perceptually distinct and make Automatic Extra Bright reachable, while safely extending AOD Control from the proven crDroid RMX3363 setup to other lunaa/RMX336* builds including Lineage-derived ROMs such as AxionOS.

## Automatic brightness
The current implementation stores separate preset curves and applies Maximum Brightness as a clamp. This makes the cap irrelevant until a curve approaches it and keeps the presets visually too close.

Use one normalized ambient-light response shape and map it into a preset-specific output envelope. Maximum Brightness is the upper end of the envelope, so changing it affects the whole automatic response instead of only clipping the last part. Default preset ceilings are separated to preserve obvious user-facing differences: DIM 25%, BALANCED 55%, BRIGHT 100%. Preset floors are 3%, 8%, and 18% respectively.

The shared response shape remains monotonic over the existing nine lux points and is deliberately more aggressive in daylight. Existing saved preset choice remains valid. Old default cap values (35/80/100) migrate to the new defaults only when they are exactly the old defaults; user-custom cap values are preserved.

## Automatic Extra Bright
Extra Bright remains available only for BRIGHT / Daylight. It uses the selected Low/Medium/Max strength as before. The old 7000-lux-for-1500-ms requirement is replaced with a reachable sunlight/daylight gate: enable at >= 1500 lux for 600 ms and disable below 700 lux for 1500 ms. Fresh pre-doze lux is valid for 15 seconds so ROMs that pause the normal light sensor after entering doze can still enter HBM safely from a recent daylight reading.

A missing, invalid, or stale lux value never enables HBM. HBM remains session-latched once the proven Oplus 0->1 edge succeeds, preserving existing safety behavior.

## Device and ROM compatibility
Normal adaptive AOD and vendor Extra Bright are gated separately.

Normal adaptive AOD may attach only when the expected SystemUI doze class/method contract exists and exactly one SensorManager, DisplayManager, and Handler can be resolved. Prefer the known field names; use a type-based fallback only when exactly one compatible field exists. Ambiguous runtime layouts fail closed.

Vendor Extra Bright requires all of the following:
- Android device/product identifies `lunaa`, or an `RMX336*` model/product together with a lunaa identity signal.
- `/sys/kernel/oplus_display/notify_fppress` exists when checked by the rooted companion app.
- `/sys/kernel/oplus_display/dimlayer_hbm` exists.
- `/sys/class/backlight/panel0-backlight/brightness` exists.
- The root broadcast sender is trusted SystemUI using the existing sender policy.

RMX3360 and RMX3363 are known lunaa models. Other RMX336* values are not assumed safe merely from the model prefix; they must pass the lunaa and capability checks.

ROM names are informational only and never whitelist HBM. AxionOS is LineageOS-derived and exposes its own optional generic HBM configuration, but AOD Control will not write that generic node. It will use only the already-proven Oplus notify_fppress path when present.

If the SystemUI contract works but the Oplus HBM capability does not, normal AOD Control remains active and Extra Bright is unavailable. If the SystemUI contract itself is incompatible, the module leaves stock behavior untouched.

## Diagnostics
QA output records device/model/product/fingerprint, ROM-identifying properties where available, resolved hook fields, HBM capability probe result, current settings including all manual/extra level percentages, current lux age, Auto Extra Bright policy state, and relevant sysfs nodes.

## Version
This compatibility/calibration release is v1.6.0 because it changes runtime compatibility architecture and Automatic Brightness semantics while preserving settings compatibility.
