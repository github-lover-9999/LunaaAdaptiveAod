package dev.lunaa.aod;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
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
    private final BrightnessSmoother smoother = new BrightnessSmoother();
    private final ExtraBrightnessPolicy extraBrightnessPolicy = new ExtraBrightnessPolicy();
    private final SensorManager sensorManager;
    private final DisplayManager displayManager;
    private final Handler handler;
    private final XposedSettingsReader settingsReader;
    private final OplusExtraBrightnessController extraBrightnessController;
    private final int displayId;

    private Sensor lightSensor;
    private boolean lightSensorRegistered;
    private boolean brightnessSampling;
    private boolean brightnessTransitionScheduled;
    private boolean pendingAmbientIntent;
    private boolean extraBrightnessEvaluationScheduled;
    private boolean destroyed;
    private float lastObservedLux = Float.NaN;
    private long lastObservedLuxMs = Long.MIN_VALUE;
    private AodSettingsSnapshot currentSettings = AodSettingsDefaults.balanced();
    private boolean automaticExtraBrightnessEnabled = true;

    private final SensorEventListener lightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (destroyed || event == null || event.values == null || event.values.length == 0) return;
            float lux = event.values[0];
            long nowMs = SystemClock.elapsedRealtime();
            if (Float.isFinite(lux) && lux >= 0f) {
                lastObservedLux = lux;
                lastObservedLuxMs = nowMs;
            }
            float target = engine.onLux(nowMs, lux);
            if (!Float.isNaN(target)) {
                retargetSmooth(target, "lux");
            }
            updateExtraBrightness(nowMs, lux);
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

    private final Runnable brightnessTransitionRunner = new Runnable() {
        @Override
        public void run() {
            brightnessTransitionScheduled = false;
            if (destroyed || !engine.isEnabled() || !engine.isAmbientActive()) return;

            long nowMs = SystemClock.elapsedRealtime();
            float value = smoother.valueAt(nowMs);
            if (!Float.isNaN(value)) {
                apply(value, "smooth");
            }
            if (smoother.isRunning(nowMs)) {
                brightnessTransitionScheduled = true;
                handler.postDelayed(this, BrightnessSmoother.FRAME_INTERVAL_MS);
            }
        }
    };

    private final Runnable udfpsRecoveryRunner = new Runnable() {
        @Override
        public void run() {
            if (destroyed || !engine.isEnabled()) return;
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
            updateExtraBrightness(SystemClock.elapsedRealtime(), lastObservedLux);
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
        this.settingsReader = settingsReader;
        this.displayId = Display.DEFAULT_DISPLAY;
        this.sensorManager = sensorManager;
        this.displayManager = displayManager;
        reloadSettings();

        if (displayManager != null) {
            displayManager.registerDisplayListener(displayListener, handler);
        }
        captureScreenBrightness();
        refreshDisplayState();
    }

    public void onDozeTransition(Object newState) {
        if (destroyed) return;
        String stateName = newState == null ? null : String.valueOf(newState);
        if ("FINISH".equals(stateName)) {
            pendingAmbientIntent = false;
            engine.clearPendingAmbientEntry();
            stopBrightnessTransition();
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
        float prepared = engine.prepareAmbientEntry(SystemClock.elapsedRealtime());
        if (!Float.isNaN(prepared)) {
            applyImmediate(prepared, "prepare-aod");
        }
    }

    public void reloadSettings() {
        if (destroyed || settingsReader == null) return;
        currentSettings = settingsReader.reload();
        automaticExtraBrightnessEnabled = settingsReader.isAutomaticExtraBrightnessEnabled();
        engine.updateSettings(currentSettings);
        if (!engine.isEnabled()) {
            pendingAmbientIntent = false;
            stopBrightnessTransition();
            unregisterLightSensor();
            stopExtraBrightnessEvaluation();
            extraBrightnessPolicy.reset();
            extraBrightnessController.forceOff();
        } else {
            updateExtraBrightness(SystemClock.elapsedRealtime(), lastObservedLux);
        }
    }

    public void captureScreenBrightness() {
        if (destroyed || displayManager == null || !engine.isEnabled() || !engine.isAutomaticMode()) return;
        try {
            Object value = XposedHelpers.callMethod(displayManager, "getBrightness", displayId);
            if (value instanceof Number) {
                engine.captureScreenBrightness(((Number) value).floatValue());
            }
        } catch (Throwable t) {
            Log.w(TAG, "Could not capture pre-doze screen brightness", t);
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
                stopBrightnessTransition();
                stopExtraBrightnessEvaluation();
                extraBrightnessPolicy.reset();
            } else {
                if (!Float.isNaN(initialTarget)) {
                    applyImmediate(initialTarget, "enter-doze");
                }
                updateExtraBrightness(nowMs, lastObservedLux);
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
        stopBrightnessTransition();
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
        long nowMs = SystemClock.elapsedRealtime();
        float target = Float.NaN;
        if (engine.isAmbientActive() || pendingAmbientIntent) {
            target = smoother.valueAt(nowMs);
        }
        if (Float.isNaN(target)) {
            target = engine.reapply();
        }
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

    private void updateExtraBrightness(long nowMs, float lux) {
        float effectiveLux = lux;
        if (currentSettings.isAutomaticMode()) {
            boolean recent = Float.isFinite(lux)
                    && lux >= 0f
                    && lastObservedLuxMs != Long.MIN_VALUE
                    && nowMs >= lastObservedLuxMs
                    && nowMs - lastObservedLuxMs <= EXTRA_BRIGHTNESS_MAX_LUX_AGE_MS;
            if (!recent) effectiveLux = Float.NaN;
        }
        boolean desired;
        if (currentSettings.isAutomaticMode() && !automaticExtraBrightnessEnabled) {
            extraBrightnessPolicy.reset();
            desired = false;
        } else {
            desired = extraBrightnessPolicy.update(
                    nowMs,
                    effectiveLux,
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
            lightSensorRegistered = sensorManager.registerListener(
                    lightListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    handler
            );
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
        }
    }

    private void applyImmediate(float target, String reason) {
        long nowMs = SystemClock.elapsedRealtime();
        stopBrightnessTransition();
        smoother.snap(nowMs, target);
        apply(target, reason);
    }

    private void retargetSmooth(float target, String reason) {
        if (destroyed || !engine.isEnabled() || !engine.isAmbientActive()) return;
        long nowMs = SystemClock.elapsedRealtime();
        smoother.retarget(nowMs, target);
        float current = smoother.valueAt(nowMs);
        if (!Float.isNaN(current)) {
            apply(current, reason + "-start");
        }
        scheduleBrightnessTransition(nowMs);
    }

    private void scheduleBrightnessTransition(long nowMs) {
        if (!smoother.isRunning(nowMs)) return;
        if (brightnessTransitionScheduled) {
            handler.removeCallbacks(brightnessTransitionRunner);
        }
        brightnessTransitionScheduled = true;
        handler.postDelayed(brightnessTransitionRunner, BrightnessSmoother.FRAME_INTERVAL_MS);
    }

    private void stopBrightnessTransition() {
        if (!brightnessTransitionScheduled) return;
        brightnessTransitionScheduled = false;
        handler.removeCallbacks(brightnessTransitionRunner);
    }

    private void apply(float target, String reason) {
        if (bridge.applyBrightness(dozeScreenBrightness, target)) {
            Log.i(TAG, "reason=" + reason + " target=" + target + " applied");
        }
    }
}
