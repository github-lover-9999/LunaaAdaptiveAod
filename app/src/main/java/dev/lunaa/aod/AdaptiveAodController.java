package dev.lunaa.aod;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import de.robv.android.xposed.XposedHelpers;

public final class AdaptiveAodController {
    private static final String TAG = "LunaaAOD";
    private static final long NORMAL_BRIGHTNESS_SAMPLE_MS = 2_000L;
    private static final long EXTRA_BRIGHTNESS_EVALUATION_MS = 500L;
    private static final long EXTRA_BRIGHTNESS_MAX_LUX_AGE_MS = 15_000L;
    private static final long UDFPS_RECOVERY_REAPPLY_MS = 400L;

    private final Object dozeScreenBrightness;
    private final DozeBridge bridge;
    private final AdaptiveAodEngine engine;
    private final ExtraBrightnessPolicy extraBrightnessPolicy = new ExtraBrightnessPolicy();
    private final DozeBrightnessOwnership brightnessOwnership = new DozeBrightnessOwnership();
    private final SensorManager sensorManager;
    private final DisplayManager displayManager;
    private final Handler handler;
    private final XposedSettingsReader settingsReader;
    private final OplusExtraBrightnessController extraBrightnessController;
    private final Context systemUiContext;
    private final int displayId;

    private Sensor lightSensor;
    private boolean lightSensorRegistered;
    private boolean brightnessSampling;
    private boolean pendingAmbientIntent;
    private boolean extraBrightnessEvaluationScheduled;
    private boolean destroyed;
    private float lastLoggedLux = Float.NaN;
    private float loggedBrightnessScale = Float.NaN;
    private float lastAppliedBrightness = Float.NaN;
    /** Doze brightness the module last wrote, whoever owned it: where the panel is now. */
    private float shownDozeBrightness = Float.NaN;
    private AodSettingsSnapshot currentSettings = AodSettingsDefaults.balanced();
    private boolean automaticExtraBrightnessEnabled = true;

    private final SensorEventListener lightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (destroyed || event == null || event.values == null || event.values.length == 0) return;
            float lux = event.values[0];
            long nowMs = SystemClock.elapsedRealtime();
            logLux(lux);
            float target = engine.onLux(nowMs, lux);
            if (!Float.isNaN(target)) {
                applyLuxTarget(target);
            }
            updateExtraBrightness(nowMs);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private final Runnable brightnessSampler = new Runnable() {
        @Override
        public void run() {
            if (destroyed || !brightnessSampling) return;
            captureScreenBrightness();
            handler.postDelayed(this, NORMAL_BRIGHTNESS_SAMPLE_MS);
        }
    };

    private final Runnable udfpsRecoveryRunner = new Runnable() {
        @Override
        public void run() {
            if (destroyed || !engine.isEnabled() || !brightnessOwnership.isOwnedByModule()) return;
            reapplyCurrentTarget("udfps-recovery");
        }
    };

    private final Runnable extraBrightnessEvaluationRunner = new Runnable() {
        @Override
        public void run() {
            extraBrightnessEvaluationScheduled = false;
            if (destroyed || !engine.isAmbientActive()) {
                extraBrightnessController.finishSession();
                return;
            }
            if (!engine.isEnabled()) {
                extraBrightnessController.forceOff();
                return;
            }
            updateExtraBrightness(SystemClock.elapsedRealtime());
            if (engine.isAutomaticMode()) scheduleExtraBrightnessEvaluation();
        }
    };

    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int id) {}

        @Override
        public void onDisplayRemoved(int id) {}

        @Override
        public void onDisplayChanged(int id) {
            if (id == displayId) refreshDisplayState();
        }
    };

    public AdaptiveAodController(Object dozeScreenBrightness, SensorManager sensorManager, DisplayManager displayManager, Handler handler, XposedSettingsReader settingsReader, Context systemUiContext) {
        this.dozeScreenBrightness = dozeScreenBrightness;
        this.bridge = new DozeBridge();
        this.engine = new AdaptiveAodEngine();
        this.handler = handler;
        this.extraBrightnessController = new OplusExtraBrightnessController(handler, systemUiContext);
        this.systemUiContext = systemUiContext;
        this.settingsReader = settingsReader;
        this.displayId = Display.DEFAULT_DISPLAY;
        this.sensorManager = sensorManager;
        this.displayManager = displayManager;
        reloadSettings();

        if (displayManager != null) {
            displayManager.registerDisplayListener(displayListener, handler);
        }
        captureScreenBrightness();
        refreshBrightnessScale();
        refreshDisplayState();
    }

    public void onDozeTransition(Object newState) {
        if (destroyed) return;
        String stateName = newState == null ? null : String.valueOf(newState);
        Log.i(TAG, "doze state=" + stateName);
        engine.setSensorsCovered(DozeStatePolicy.isSensorCoveredState(stateName),
                SystemClock.elapsedRealtime());
        if (brightnessOwnership.onDozeState(stateName)) {
            handBackToStock(stateName);
        }
        if ("FINISH".equals(stateName)) {
            pendingAmbientIntent = false;
            engine.clearPendingAmbientEntry();
            stopExtraBrightnessEvaluation();
            extraBrightnessPolicy.reset();
            extraBrightnessController.finishSession();
            return;
        }
        if (!DozeStatePolicy.isAmbientIntentState(stateName)) return;

        reloadSettings();
        if (!engine.isEnabled()) {
            pendingAmbientIntent = false;
            engine.clearPendingAmbientEntry();
            unregisterLightSensor();
            stopExtraBrightnessEvaluation();
            extraBrightnessPolicy.reset();
            extraBrightnessController.forceOff();
            return;
        }

        pendingAmbientIntent = true;
        refreshBrightnessScale();
        float prepared = engine.prepareAmbientEntry(SystemClock.elapsedRealtime());
        if (!reclaimFromStock(readDisplayState())) {
            Log.i(TAG, "prepare-aod deferred until the display leaves full power");
            if (brightnessOwnership.isScreenOffAnimation()) holdScreenBrightnessForScreenOffAnimation();
            return;
        }
        if (!Float.isNaN(prepared)) {
            apply(prepared, "prepare-aod");
        }
    }

    /**
     * SystemUI is taking the panel out of AOD (UDFPS touch, notification pulse or wake-up).
     * Before the display switches to full power, leave a doze brightness no brighter than the
     * normal screen: stock SystemUI shows it on the pulse and forces it onto the unlock window.
     */
    private void handBackToStock(String stateName) {
        pendingAmbientIntent = false;
        engine.clearPendingAmbientEntry();
        handler.removeCallbacks(udfpsRecoveryRunner);
        stopExtraBrightnessEvaluation();
        extraBrightnessPolicy.reset();
        float handback = DozeBrightnessOwnership.handbackBrightness(
                lastAppliedBrightness, readScreenBrightness());
        long handbackRampMs = DozeRamp.durationMs(shownDozeBrightness, handback, ValueAnimator.getDurationScale());
        extraBrightnessController.yieldToStockFingerprint(handbackRampMs);
        if (!Float.isNaN(handback) && bridge.applyBrightness(dozeScreenBrightness, handback)) {
            shownDozeBrightness = handback;
            Log.i(TAG, "reason=handback-" + stateName + " target=" + handback + " applied");
        }
    }

    /**
     * Stock resets the doze brightness to its dim default and shows it on the full-power
     * screen-off animation. Keep the brightness the user just saw instead: the AOD target
     * follows in one step once the display has entered AOD.
     */
    private void holdScreenBrightnessForScreenOffAnimation() {
        float brightness = DozeBrightnessOwnership.screenOffAnimationBrightness(
                readShownScreenBrightness(), readScreenBrightness());
        if (!Float.isNaN(brightness) && bridge.applyBrightness(dozeScreenBrightness, brightness)) {
            shownDozeBrightness = brightness;
            Log.i(TAG, "reason=screen-off-animation target=" + brightness + " applied");
        }
    }

    /** @return true when the module owns the doze brightness, reclaiming it once AOD is back at low power */
    private boolean reclaimFromStock(int displayState) {
        if (brightnessOwnership.isOwnedByModule()) return true;
        if (!brightnessOwnership.reclaim(displayState)) return false;
        Log.i(TAG, "doze brightness reclaimed from stock displayState=" + displayState);
        extraBrightnessController.resumeAfterStockFingerprint(SystemClock.elapsedRealtime());
        return true;
    }

    public void reloadSettings() {
        if (destroyed || settingsReader == null) return;
        currentSettings = settingsReader.reload();
        automaticExtraBrightnessEnabled = settingsReader.isAutomaticExtraBrightnessEnabled();
        engine.updateSettings(currentSettings);
        if (!engine.isEnabled()) {
            pendingAmbientIntent = false;
            unregisterLightSensor();
            stopExtraBrightnessEvaluation();
            extraBrightnessPolicy.reset();
            extraBrightnessController.forceOff();
        } else {
            updateExtraBrightness(SystemClock.elapsedRealtime());
        }
    }

    public void captureScreenBrightness() {
        if (destroyed || displayManager == null || !engine.isEnabled() || !engine.isAutomaticMode()) return;
        float value = readScreenBrightness();
        if (!Float.isNaN(value)) {
            engine.captureScreenBrightness(value);
        }
    }

    /** Normal (non-doze) screen brightness, as stock DozeScreenBrightness reads it for its clamp. */
    private float readScreenBrightness() {
        if (displayManager == null) return Float.NaN;
        try {
            Object value = XposedHelpers.callMethod(displayManager, "getBrightness", displayId);
            return value instanceof Number ? ((Number) value).floatValue() : Float.NaN;
        } catch (Throwable t) {
            Log.w(TAG, "Could not read screen brightness", t);
            return Float.NaN;
        }
    }

    /**
     * Brightness DisplayPowerController currently targets for the normal screen, including the
     * dimming before a timeout (hidden {@code BrightnessInfo.adjustedBrightness}).
     */
    private float readShownScreenBrightness() {
        try {
            return brightnessInfoField("adjustedBrightness");
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the shown screen brightness; using the setting: " + t);
            return Float.NaN;
        }
    }

    /** @return a float field of the hidden {@code BrightnessInfo}, NaN when there is none */
    private float brightnessInfoField(String name) {
        if (displayManager == null) return Float.NaN;
        Display display = displayManager.getDisplay(displayId);
        Object info = display == null ? null : XposedHelpers.callMethod(display, "getBrightnessInfo");
        Object value = info == null ? null : XposedHelpers.getObjectField(info, name);
        return value instanceof Number ? ((Number) value).floatValue() : Float.NaN;
    }

    /**
     * Manual levels are shares of the brightest normal AOD brightness. Read it each time AOD
     * starts: the HBM transition point Android clamps AOD at, and the brightness slider maximum,
     * which marks the same end of the normal range when the ROM has no HBM data.
     */
    private void refreshBrightnessScale() {
        float transitionPoint = Float.NaN;
        float maximum = Float.NaN;
        try {
            transitionPoint = brightnessInfoField("highBrightnessTransitionPoint");
            maximum = brightnessInfoField("brightnessMaximum");
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the display brightness limits: " + t);
        }
        float sliderMaximum = readSliderMaximum();
        float scale = AodBrightnessScale.fullScale(transitionPoint, sliderMaximum);
        engine.setBrightnessScale(scale);
        if (scale != loggedBrightnessScale) {
            loggedBrightnessScale = scale;
            Log.i(TAG, "aod brightness scale=" + scale + " hbmTransitionPoint=" + transitionPoint
                    + " sliderMax=" + sliderMaximum + " displayMax=" + maximum);
        }
    }

    /** {@code config_screenBrightnessSettingMaximumFloat}, with the device overlay applied. */
    private float readSliderMaximum() {
        try {
            Resources res = systemUiContext == null ? null : systemUiContext.getResources();
            int id = res == null ? 0
                    : res.getIdentifier("config_screenBrightnessSettingMaximumFloat", "dimen", "android");
            if (id == 0) return Float.NaN;
            TypedValue value = new TypedValue();
            res.getValue(id, value, true);
            return value.type == TypedValue.TYPE_FLOAT ? value.getFloat() : Float.NaN;
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the brightness slider maximum: " + t);
            return Float.NaN;
        }
    }

    private int readDisplayState() {
        if (displayManager == null) return Display.STATE_UNKNOWN;
        try {
            Display display = displayManager.getDisplay(displayId);
            return display == null ? Display.STATE_UNKNOWN : display.getState();
        } catch (Throwable t) {
            Log.w(TAG, "Could not read display state", t);
            return Display.STATE_UNKNOWN;
        }
    }

    public void refreshDisplayState() {
        if (destroyed || displayManager == null) return;
        try {
            Display display = displayManager.getDisplay(displayId);
            if (display == null) return;
            int displayState = display.getState();
            boolean ambient = DisplayStatePolicy.isAmbientState(displayState);
            boolean displayOn = displayState == Display.STATE_ON;

            if (engine.isEnabled() && engine.isAutomaticMode()) {
                updateBrightnessSampler(displayState);
                if (!ambient && displayOn) {
                    captureScreenBrightness();
                }
            } else {
                stopBrightnessSampler();
            }

            boolean changed = engine.isAmbientActive() != ambient;
            long nowMs = SystemClock.elapsedRealtime();
            float initialTarget = engine.setAmbientActive(ambient, nowMs);
            extraBrightnessController.setAmbientActive(ambient, nowMs);
            boolean sensorNeeded = engine.shouldObserveLux(displayOn, ambient);
            if (sensorNeeded) {
                registerLightSensor();
            } else {
                unregisterLightSensor();
            }

            if (!ambient) {
                stopExtraBrightnessEvaluation();
                extraBrightnessPolicy.reset();
            } else {
                boolean handedBack = !brightnessOwnership.isOwnedByModule();
                if (reclaimFromStock(displayState) && handedBack && Float.isNaN(initialTarget)) {
                    reapplyBrightnessTarget("reclaim");
                }
                if (!Float.isNaN(initialTarget)) {
                    applyEntry(initialTarget);
                }
                updateExtraBrightness(nowMs);
                scheduleExtraBrightnessEvaluation();
            }

            if (changed) {
                Log.i(TAG, "displayState=" + displayState
                        + " enabled=" + engine.isEnabled()
                        + " mode=" + (engine.isAutomaticMode() ? "AUTO" : "MANUAL")
                        + " ambient=" + ambient);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Display-state refresh failed; stock behavior retained", t);
        }
    }

    public void reapplyAfterReset() {
        if (destroyed) return;
        if (brightnessOwnership.isScreenOffAnimation()) holdScreenBrightnessForScreenOffAnimation();
        // Refresh first while the prior latch is still visible to the policy. This avoids
        // scheduling a synthetic HBM edge from inside display-state refresh itself.
        refreshDisplayState();
        if (!engine.isEnabled()) {
            unregisterLightSensor();
            handler.removeCallbacks(udfpsRecoveryRunner);
            return;
        }

        extraBrightnessController.onStockBrightnessReset();
        reapplyBrightnessTarget("after-stock-reset");
        handler.removeCallbacks(udfpsRecoveryRunner);
        handler.postDelayed(udfpsRecoveryRunner, UDFPS_RECOVERY_REAPPLY_MS);
    }

    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        handler.removeCallbacks(udfpsRecoveryRunner);
        stopBrightnessSampler();
        stopExtraBrightnessEvaluation();
        extraBrightnessPolicy.reset();
        extraBrightnessController.finishSession();
        unregisterLightSensor();
        if (displayManager != null) {
            try {
                displayManager.unregisterDisplayListener(displayListener);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to unregister display listener", t);
            }
        }
        pendingAmbientIntent = false;
        engine.clearPendingAmbientEntry();
        engine.setAmbientActive(false, SystemClock.elapsedRealtime());
        Log.i(TAG, "controller destroyed");
    }

    private void reapplyBrightnessTarget(String reason) {
        float target = engine.reapply();
        if (Float.isNaN(target) && pendingAmbientIntent) {
            target = engine.pendingTarget();
        }
        if (!Float.isNaN(target)) {
            apply(target, reason);
        }
    }

    private void reapplyCurrentTarget(String reason) {
        reapplyBrightnessTarget(reason);
        extraBrightnessController.reassertIfDesired();
    }

    private void updateExtraBrightness(long nowMs) {
        // Manual Extra Bright does not depend on the light; automatic needs a current reading.
        float effectiveLux = engine.currentLux(nowMs, EXTRA_BRIGHTNESS_MAX_LUX_AGE_MS);
        boolean desired;
        if (!brightnessOwnership.isOwnedByModule()
                || (currentSettings.isAutomaticMode() && !automaticExtraBrightnessEnabled)) {
            extraBrightnessPolicy.reset();
            desired = false;
        } else {
            desired = extraBrightnessPolicy.update(
                    nowMs,
                    effectiveLux,
                    engine.luxAtLeastSinceMs(ExtraBrightnessPolicy.ENABLE_LUX),
                    currentSettings,
                    engine.isAmbientActive()
            );
        }
        if (currentSettings.isAutomaticMode() && currentSettings.getPreset() == AodPreset.BRIGHT
                && Float.isFinite(effectiveLux) && effectiveLux >= 400f) {
            Log.i(TAG, "extraBright auto-eval: lux=" + effectiveLux
                    + " threshold=" + ExtraBrightnessPolicy.ENABLE_LUX
                    + " switch=" + automaticExtraBrightnessEnabled
                    + " desired=" + desired);
        }
        extraBrightnessController.setDesired(desired, currentSettings.getExtraBrightPercent());
    }

    private void scheduleExtraBrightnessEvaluation() {
        if (destroyed || extraBrightnessEvaluationScheduled || !engine.isAmbientActive()
                || !engine.isEnabled() || !engine.isAutomaticMode()) return;
        extraBrightnessEvaluationScheduled = true;
        handler.postDelayed(extraBrightnessEvaluationRunner, EXTRA_BRIGHTNESS_EVALUATION_MS);
    }

    private void stopExtraBrightnessEvaluation() {
        if (!extraBrightnessEvaluationScheduled) return;
        extraBrightnessEvaluationScheduled = false;
        handler.removeCallbacks(extraBrightnessEvaluationRunner);
    }

    private void updateBrightnessSampler(int displayState) {
        if (displayState == Display.STATE_ON) {
            if (!brightnessSampling) {
                brightnessSampling = true;
                handler.post(brightnessSampler);
            }
        } else {
            stopBrightnessSampler();
        }
    }

    private void stopBrightnessSampler() {
        if (!brightnessSampling) return;
        brightnessSampling = false;
        handler.removeCallbacks(brightnessSampler);
    }

    private void registerLightSensor() {
        if (lightSensorRegistered || sensorManager == null || destroyed) return;
        try {
            if (lightSensor == null) {
                // Keep the proven real-lux path. qti.sensor.lux_aod has an undocumented OPlus payload.
                lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT, false);
                if (lightSensor == null) {
                    lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                }
            }
            if (lightSensor == null) {
                Log.w(TAG, "TYPE_LIGHT unavailable; using remembered/fallback AOD target");
                return;
            }
            engine.setLuxTracking(true, SystemClock.elapsedRealtime());
            lightSensorRegistered = sensorManager.registerListener(
                    lightListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    handler
            );
            if (!lightSensorRegistered) engine.setLuxTracking(false, SystemClock.elapsedRealtime());
            lastLoggedLux = Float.NaN;
            Log.i(TAG, "lightSensorRegistered=" + lightSensorRegistered);
        } catch (Throwable t) {
            Log.e(TAG, "Light sensor registration failed; using fallback target", t);
        }
    }

    private void unregisterLightSensor() {
        if (!lightSensorRegistered || sensorManager == null) return;
        try {
            sensorManager.unregisterListener(lightListener);
        } catch (Throwable t) {
            Log.w(TAG, "Failed to unregister light sensor", t);
        } finally {
            lightSensorRegistered = false;
            engine.setLuxTracking(false, SystemClock.elapsedRealtime());
        }
    }

    /** One line per noticeable change: enough to follow the light without flooding the log. */
    private void logLux(float lux) {
        if (!Float.isFinite(lux) || lux < 0f) return;
        if (!Float.isNaN(lastLoggedLux)
                && Math.abs(lux - lastLoggedLux) < Math.max(5f, lastLoggedLux * 0.25f)) return;
        lastLoggedLux = lux;
        Log.i(TAG, "lux=" + Math.round(lux) + " ambient=" + engine.isAmbientActive()
                + (engine.areSensorsCovered() ? " ignored: sensors covered" : ""));
    }

    /**
     * Shows the AOD target in one step. AOD refreshes the panel too rarely for a module-driven
     * rise to look smooth. Without the System Framework hook the system still ramps the change;
     * Extra Bright waits until that ramp is over.
     */
    private void applyEntry(float target) {
        float from = shownDozeBrightness;
        apply(target, "enter-doze");
        float animatorScale = ValueAnimator.getDurationScale();
        long rampMs = DozeRamp.durationMs(from, target, animatorScale);
        Log.i(TAG, "reason=enter-doze from=" + from + " systemRampMs=" + rampMs
                + " animatorScale=" + animatorScale);
        extraBrightnessController.setEntryRampMs(rampMs);
    }

    /** A light change lands in one step as well; {@link UpdateGate} keeps the steps visible and rare. */
    private void applyLuxTarget(float target) {
        if (destroyed || !engine.isEnabled() || !engine.isAmbientActive()) return;
        apply(target, "lux");
    }

    private void apply(float target, String reason) {
        if (write(target)) {
            Log.i(TAG, "reason=" + reason + " target=" + target + " applied");
        }
    }

    private boolean write(float target) {
        if (!brightnessOwnership.isOwnedByModule()) return false;
        if (!bridge.applyBrightness(dozeScreenBrightness, target)) return false;
        lastAppliedBrightness = target;
        shownDozeBrightness = target;
        return true;
    }
}
