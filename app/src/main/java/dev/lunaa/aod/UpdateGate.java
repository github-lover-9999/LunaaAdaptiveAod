package dev.lunaa.aod;

/**
 * Decides which light changes reach the AOD brightness. Each one lands in a single step, so it
 * has to be a real change in the light and a visible change in brightness.
 */
public final class UpdateGate {
    static final float MIN_ABS_LUX_DELTA = 10f;
    static final float MIN_RELATIVE_LUX_DELTA = 0.08f;
    static final float MIN_TARGET_DELTA = 0.005f;
    /**
     * In the perceived (HLG) scale, about 1% of it: near the top of the range a linear step of
     * {@link #MIN_TARGET_DELTA} is invisible, and each written step costs a display update.
     */
    static final float MIN_PERCEIVED_DELTA = 0.01f;
    static final long FORCE_AFTER_MS = 2000L;

    private boolean hasApplied;
    private float lastLux;
    private float lastTarget;
    private long lastAppliedMs;

    public boolean shouldApply(long nowMs, float lux, float target) {
        if (!hasApplied) {
            record(nowMs, lux, target);
            return true;
        }

        float targetDelta = Math.abs(target - lastTarget);
        float perceivedDelta = Math.abs(DozeRamp.hlg(target) - DozeRamp.hlg(lastTarget));
        if (targetDelta < MIN_TARGET_DELTA || perceivedDelta < MIN_PERCEIVED_DELTA) {
            return false;
        }

        float absLuxDelta = Math.abs(lux - lastLux);
        float denominator = Math.max(1f, Math.abs(lastLux));
        float relativeLuxDelta = absLuxDelta / denominator;
        boolean luxMoved = absLuxDelta >= MIN_ABS_LUX_DELTA
                || relativeLuxDelta >= MIN_RELATIVE_LUX_DELTA;
        boolean timedOut = nowMs - lastAppliedMs >= FORCE_AFTER_MS;
        if (!luxMoved && !timedOut) {
            return false;
        }

        record(nowMs, lux, target);
        return true;
    }

    public void record(long nowMs, float lux, float target) {
        hasApplied = true;
        lastLux = lux;
        lastTarget = target;
        lastAppliedMs = nowMs;
    }

    public void reset() {
        hasApplied = false;
    }
}
