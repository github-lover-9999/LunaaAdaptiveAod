package dev.lunaa.aod;

/**
 * Time-based linear smoothing for AOD display-request brightness.
 * Brightening is intentionally faster than darkening.
 */
public final class BrightnessSmoother {
    public static final long BRIGHTEN_DURATION_MS = 400L;
    public static final long DARKEN_DURATION_MS = 1400L;
    public static final long FRAME_INTERVAL_MS = 100L;

    private float startValue = Float.NaN;
    private float targetValue = Float.NaN;
    private long startMs;
    private long durationMs;

    public void snap(long nowMs, float value) {
        float clamped = clamp(value);
        startValue = clamped;
        targetValue = clamped;
        startMs = nowMs;
        durationMs = 0L;
    }

    public void retarget(long nowMs, float target) {
        float clampedTarget = clamp(target);
        float current = valueAt(nowMs);
        if (Float.isNaN(current)) {
            snap(nowMs, clampedTarget);
            return;
        }

        startValue = current;
        targetValue = clampedTarget;
        startMs = nowMs;
        if (clampedTarget > current) {
            durationMs = BRIGHTEN_DURATION_MS;
        } else if (clampedTarget < current) {
            durationMs = DARKEN_DURATION_MS;
        } else {
            durationMs = 0L;
        }
    }

    public float valueAt(long nowMs) {
        if (Float.isNaN(targetValue)) {
            return Float.NaN;
        }
        if (durationMs <= 0L || nowMs <= startMs) {
            return startValue;
        }
        long elapsed = nowMs - startMs;
        if (elapsed >= durationMs) {
            return targetValue;
        }
        float ratio = (float) elapsed / (float) durationMs;
        return startValue + (targetValue - startValue) * ratio;
    }

    public boolean isRunning(long nowMs) {
        return !Float.isNaN(targetValue)
                && durationMs > 0L
                && nowMs >= startMs
                && nowMs - startMs < durationMs;
    }

    public float target() {
        return targetValue;
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("brightness must be finite");
        }
        return Math.max(
                AodSettingsSnapshot.MIN_BRIGHTNESS,
                Math.min(AodSettingsSnapshot.MAX_BRIGHTNESS, value)
        );
    }
}
