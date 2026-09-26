package dev.lunaa.aod;

public final class AdaptiveAodEngine {
    private static final long RECENT_LUX_MS = 10_000L;
    static final long ENTRY_DARK_GUARD_MS = 1_200L;
    private static final int REQUIRED_DARK_SAMPLES = 2;
    private static final float DARK_SAMPLE_MIN_TOLERANCE = 0.010f;
    private static final float DARK_SAMPLE_RELATIVE_TOLERANCE = 0.25f;
    /** The hand may reach the light sensor this long before the proximity sensor pauses AOD. */
    static final long COVER_LEAD_MS = 1_000L;
    private static final int LUX_HISTORY = 8;

    private final UpdateGate gate = new UpdateGate();
    private AodSettingsSnapshot settings = AodSettingsDefaults.balanced();
    private boolean ambientActive;
    private float lastScreenBrightness = Float.NaN;
    private float lastLux = Float.NaN;
    private long lastLuxMs = Long.MIN_VALUE;
    private boolean luxTracking;
    /** A reading arrived since the light sensor was registered and it is still registered. */
    private boolean luxLive;
    private boolean sensorsCovered;
    private final float[] luxHistory = new float[LUX_HISTORY];
    private final long[] luxHistoryMs = new long[LUX_HISTORY];
    private int luxHistoryCount;
    private int luxHistoryNext;
    /** Brightest normal AOD brightness; Manual levels and automatic targets are shares of it. */
    private float brightnessScale = 1f;
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

    /** @param fullScale see {@link AodBrightnessScale}; NaN or out of range keeps the full range */
    public void setBrightnessScale(float fullScale) {
        brightnessScale = Float.isFinite(fullScale) && fullScale > 0f && fullScale <= 1f ? fullScale : 1f;
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

    /**
     * The light sensor listener was registered or unregistered. The light sensor reports only
     * changes, so while it stays registered its last reading is still the current light.
     */
    public void setLuxTracking(boolean tracking, long nowMs) {
        if (!tracking && luxTracking && luxLive) {
            lastLuxMs = nowMs;
        }
        luxTracking = tracking;
        luxLive = false;
    }

    /**
     * AOD is paused by the proximity sensor: the light sensor is covered, not reading the room.
     * Readings from just before the pause already saw the hand, so the room light from before
     * them is kept.
     */
    public void setSensorsCovered(boolean covered, long nowMs) {
        if (covered && !sensorsCovered) restoreLuxFromBefore(nowMs - COVER_LEAD_MS);
        sensorsCovered = covered;
    }

    public boolean areSensorsCovered() {
        return sensorsCovered;
    }

    /** Current ambient light, or NaN when it is unknown, older than {@code maxAgeMs} or covered. */
    public float currentLux(long nowMs, long maxAgeMs) {
        return !sensorsCovered && hasLuxWithin(nowMs, maxAgeMs) ? lastLux : Float.NaN;
    }

    /**
     * Start of the current run of readings at or above {@code threshold}: the light has been that
     * bright at least since then. The sensor reports only changes, so a steady bright light has
     * one reading, often from before AOD. {@link Long#MAX_VALUE} when the latest reading is below
     * the threshold or there is none.
     */
    public long luxAtLeastSinceMs(float threshold) {
        long since = Long.MAX_VALUE;
        for (int i = 1; i <= luxHistoryCount; i++) {
            int index = (luxHistoryNext - i + LUX_HISTORY) % LUX_HISTORY;
            if (!(luxHistory[index] >= threshold)) break;
            since = luxHistoryMs[index];
        }
        return since;
    }

    public float onLux(long nowMs, float lux) {
        if (!Float.isFinite(lux) || lux < 0f || sensorsCovered) {
            return Float.NaN;
        }
        lastLux = lux;
        lastLuxMs = nowMs;
        luxLive = luxTracking;
        luxHistory[luxHistoryNext] = lux;
        luxHistoryMs[luxHistoryNext] = nowMs;
        luxHistoryNext = (luxHistoryNext + 1) % LUX_HISTORY;
        luxHistoryCount = Math.min(LUX_HISTORY, luxHistoryCount + 1);
        if (!ambientActive || !settings.isEnabled() || settings.isManualMode()) {
            return Float.NaN;
        }

        float target = automaticTarget(lux);
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
        if (settings.isManualMode()) return scaled(settings.getManualBrightness());
        return hasRecentLux(nowMs)
                ? automaticTarget(lastLux)
                : scaled(BrightnessCurve.initialFromScreenBrightness(lastScreenBrightness, settings));
    }

    private float automaticTarget(float lux) {
        return scaled(BrightnessCurve.targetForLux(lux, settings));
    }

    /** A share of the brightest normal AOD brightness as a display brightness. */
    private float scaled(float share) {
        return Math.max(AodSettingsSnapshot.MIN_BRIGHTNESS, share * brightnessScale);
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

    private void restoreLuxFromBefore(long beforeMs) {
        boolean recentChange = false;
        for (int i = 1; i <= luxHistoryCount; i++) {
            int index = (luxHistoryNext - i + LUX_HISTORY) % LUX_HISTORY;
            if (luxHistoryMs[index] > beforeMs) {
                recentChange = true;
                continue;
            }
            if (!recentChange) return;
            lastLux = luxHistory[index];
            if (ambientActive && settings.isEnabled() && !settings.isManualMode()) {
                currentTarget = automaticTarget(lastLux);
            }
            return;
        }
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
        return hasLuxWithin(nowMs, RECENT_LUX_MS);
    }

    private boolean hasLuxWithin(long nowMs, long maxAgeMs) {
        if (Float.isNaN(lastLux)) return false;
        if (luxLive) return true;
        return lastLuxMs != Long.MIN_VALUE
                && nowMs >= lastLuxMs
                && nowMs - lastLuxMs <= maxAgeMs;
    }
}
