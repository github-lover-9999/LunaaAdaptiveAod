package dev.lunaa.aod;

public final class AdaptiveAodEngine {
    private static final long RECENT_LUX_MS = 10_000L;
    static final long ENTRY_DARK_GUARD_MS = 1_200L;
    private static final int REQUIRED_DARK_SAMPLES = 2;
    private static final float DARK_SAMPLE_MIN_TOLERANCE = 0.010f;
    private static final float DARK_SAMPLE_RELATIVE_TOLERANCE = 0.25f;

    private final UpdateGate gate = new UpdateGate();
    private AodSettingsSnapshot settings = AodSettingsDefaults.balanced();
    private boolean ambientActive;
    private float lastScreenBrightness = Float.NaN;
    private float lastLux = Float.NaN;
    private long lastLuxMs = Long.MIN_VALUE;
    private float currentTarget = Float.NaN;
    private float pendingTarget = Float.NaN;
    private long entryGuardStartMs = Long.MIN_VALUE;
    private int guardedDarkSamples;
    private float guardedDarkTarget = Float.NaN;

    public void updateSettings(AodSettingsSnapshot newSettings) {
        settings = newSettings != null ? newSettings : AodSettingsDefaults.disabledBalanced();
        gate.reset();
        pendingTarget = Float.NaN;
        resetEntryGuard();
        if (!settings.isEnabled()) {
            currentTarget = Float.NaN;
        }
    }

    public float prepareAmbientEntry(long nowMs) {
        if (!settings.isEnabled()) {
            pendingTarget = Float.NaN;
            resetEntryGuard();
            return Float.NaN;
        }
        startEntryGuard(nowMs);
        pendingTarget = entryTarget(nowMs);
        return pendingTarget;
    }

    public void clearPendingAmbientEntry() {
        pendingTarget = Float.NaN;
    }

    public float pendingTarget() {
        return settings.isEnabled() ? pendingTarget : Float.NaN;
    }

    public void captureScreenBrightness(float brightness) {
        if (ambientActive) return;
        if (Float.isFinite(brightness) && brightness >= 0f) {
            lastScreenBrightness = brightness;
        }
    }

    public float setAmbientActive(boolean active, long nowMs) {
        if (ambientActive == active) {
            return Float.NaN;
        }
        ambientActive = active;
        gate.reset();
        if (!active) {
            currentTarget = Float.NaN;
            resetEntryGuard();
            return Float.NaN;
        }
        if (!settings.isEnabled()) {
            currentTarget = Float.NaN;
            resetEntryGuard();
            return Float.NaN;
        }

        startEntryGuard(nowMs);
        boolean recentLux = hasRecentLux(nowMs);
        currentTarget = entryTarget(nowMs);
        pendingTarget = currentTarget;
        gate.record(nowMs, recentLux ? lastLux : 0f, currentTarget);
        return currentTarget;
    }

    public float onLux(long nowMs, float lux) {
        if (!Float.isFinite(lux) || lux < 0f) {
            return Float.NaN;
        }
        lastLux = lux;
        lastLuxMs = nowMs;
        if (!ambientActive || !settings.isEnabled() || settings.isManualMode()) {
            return Float.NaN;
        }

        float target = BrightnessCurve.targetForLux(lux, settings);
        if (shouldGuardDarkening(nowMs, target)) {
            return Float.NaN;
        }
        if (!gate.shouldApply(nowMs, lux, target)) {
            return Float.NaN;
        }
        currentTarget = target;
        return target;
    }

    public float reapply() {
        return ambientActive && settings.isEnabled() ? currentTarget : Float.NaN;
    }

    public boolean isAmbientActive() {
        return ambientActive;
    }

    public boolean isEnabled() {
        return settings.isEnabled();
    }

    public boolean isAutomaticMode() {
        return settings.isAutomaticMode();
    }

    public boolean shouldObserveLux(boolean displayOn, boolean ambient) {
        return settings.isEnabled()
                && settings.isAutomaticMode()
                && (displayOn || ambient);
    }

    public float currentTarget() {
        return currentTarget;
    }

    private float entryTarget(long nowMs) {
        if (settings.isManualMode()) {
            return settings.getManualBrightness();
        }
        return hasRecentLux(nowMs)
                ? BrightnessCurve.targetForLux(lastLux, settings)
                : BrightnessCurve.initialFromScreenBrightness(lastScreenBrightness, settings);
    }

    private boolean shouldGuardDarkening(long nowMs, float target) {
        if (entryGuardStartMs == Long.MIN_VALUE
                || nowMs < entryGuardStartMs
                || nowMs - entryGuardStartMs >= ENTRY_DARK_GUARD_MS
                || !Float.isFinite(currentTarget)
                || target >= currentTarget - UpdateGate.MIN_TARGET_DELTA) {
            clearGuardedDarkSamples();
            return false;
        }

        if (guardedDarkSamples == 0 || !Float.isFinite(guardedDarkTarget)) {
            guardedDarkSamples = 1;
            guardedDarkTarget = target;
            return true;
        }

        float tolerance = Math.max(
                DARK_SAMPLE_MIN_TOLERANCE,
                Math.abs(guardedDarkTarget) * DARK_SAMPLE_RELATIVE_TOLERANCE
        );
        if (Math.abs(target - guardedDarkTarget) > tolerance) {
            guardedDarkSamples = 1;
            guardedDarkTarget = target;
            return true;
        }

        guardedDarkSamples++;
        guardedDarkTarget = target;
        if (guardedDarkSamples < REQUIRED_DARK_SAMPLES) {
            return true;
        }
        clearGuardedDarkSamples();
        return false;
    }

    private void startEntryGuard(long nowMs) {
        entryGuardStartMs = nowMs;
        clearGuardedDarkSamples();
    }

    private void resetEntryGuard() {
        entryGuardStartMs = Long.MIN_VALUE;
        clearGuardedDarkSamples();
    }

    private void clearGuardedDarkSamples() {
        guardedDarkSamples = 0;
        guardedDarkTarget = Float.NaN;
    }

    private boolean hasRecentLux(long nowMs) {
        return !Float.isNaN(lastLux)
                && lastLuxMs != Long.MIN_VALUE
                && nowMs >= lastLuxMs
                && nowMs - lastLuxMs <= RECENT_LUX_MS;
    }
}
