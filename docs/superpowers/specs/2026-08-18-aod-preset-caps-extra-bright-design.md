# Lunaa Adaptive AOD v1.4.0 Design

## Goal

Make AOD brightness understandable without technical controls: Automatic uses Dim/Balanced/Bright presets driven by the ambient light sensor, each preset has one user-adjustable maximum brightness, and Manual uses one fixed brightness slider.

## User model

### Automatic

- Dim cap: 0–40%, default 35%.
- Balanced cap: 40–80%, default 80%.
- Bright cap: 70–100%, default 100%.
- The slider is a maximum, not a fixed output. Ambient light still determines the current target below that cap.
- Balanced must reach a clearly visible daytime level without using vendor AOD-HBM.
- Dim and Balanced never request Extra Bright.
- Bright 70–89% uses only normal AOD brightness.
- Bright 90–100% permits Extra Bright, but only in strong daylight.

### Manual

- One 0–100% slider.
- Ambient light does not affect output.
- Manual never forces vendor AOD-HBM; 100% means the maximum normal Android AOD request.

## Automatic curves

Lux anchors remain `0, 2, 10, 50, 200, 500, 1000, 5000, 20000`.

- Dim: `8, 8, 12, 18, 24, 28, 32, 35, 40%`.
- Balanced: `12, 12, 18, 28, 40, 50, 55, 70, 80%`.
- Bright: `18, 18, 25, 35, 50, 60, 65, 80, 100%`.

The selected preset's cap clamps the interpolated target. The internal minimum remains 1% to avoid treating a 0% UI cap as a panel-off command.

## Extra Bright policy

Extra Bright is device-specific to the Oplus panel path already proven on lunaa.

Eligibility requires all of:

- module enabled;
- Automatic mode;
- Bright preset;
- Bright cap >= 90%;
- display in AOD/ambient state.

Ambient policy:

- arm after lux >= 7000 for 1.5 seconds;
- remain active through the hysteresis band;
- disable after lux < 4000 for 2 seconds;
- disable immediately when eligibility is lost or AOD exits.

The driver control uses `/sys/kernel/oplus_display/notify_fppress`, because Oplus kernel source shows that writing the pressed state while in AOD sends `DSI_CMD_AOD_HBM_ON`. The controller first attempts a direct write and falls back to a short `su -c` write for rooted lunaa builds. Failures must retain normal AOD behavior and never crash SystemUI.

Extra Bright is reasserted after the existing stock reset hook because a real UDFPS cycle can restore normal AOD mode.

## UI

Top-level layout:

1. title and short explanation;
2. master Enable switch;
3. Automatic / Manual segmented controls;
4. Current output card;
5. mode-specific controls;
6. sticky Reset / Save actions above navigation insets.

Automatic panel:

- three preset buttons;
- one `Maximum brightness` slider for the selected preset;
- percent value;
- short selected-preset description;
- Bright shows `Extra Bright from 90%` helper text.

Manual panel:

- one `Manual brightness` slider;
- percent value;
- no lux curve, multiplier, minimum floor, or exact normalized-value editor.

All controls keep >=48dp touch targets and the existing system-bars/display-cutout/IME inset handling.

## Compatibility and failure behavior

- Xposed scope remains only `com.android.systemui`.
- No constructor hooks, `mContext`, `ActivityThread`, qti `lux_aod`, or UDFPS class hook.
- Normal `android.sensor.light` remains the ambient source.
- If Extra Bright cannot access the Oplus control node, the module logs the failure and continues at normal maximum AOD brightness.
- Turning the master switch off stops brightness writes and explicitly requests Extra Bright off.

### Top scroll spacing amendment

The settings scroll content keeps 32dp of content spacing above the title in addition to the status-bar/display-cutout inset, so reaching/overscrolling the top does not leave the title cramped against the system bar.
