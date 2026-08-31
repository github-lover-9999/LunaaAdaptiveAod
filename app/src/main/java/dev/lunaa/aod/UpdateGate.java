package dev.lunaa.aod;

public final class UpdateGate {
    static final float MIN_ABS_LUX_DELTA = 10f;
    static final float MIN_RELATIVE_LUX_DELTA = 0.08f;
    static final float MIN_TARGET_DELTA = 0.005f;
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
        if (targetDelta < MIN_TARGET_DELTA) {
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
