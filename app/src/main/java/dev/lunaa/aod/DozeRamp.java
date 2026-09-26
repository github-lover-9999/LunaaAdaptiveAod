package dev.lunaa.aod;

/**
 * DisplayPowerController's ramp for a doze brightness change on lunaa, and when Extra Bright may
 * follow it.
 *
 * <p>lunaa's display config (display_id_4630946741972277890.xml) ramps every brightness change at
 * 0.06 per second in the perceived (HLG) scale, cut to at most 3 s and stretched by the animator
 * duration scale (instant at scale 0). An AOD brightness change of the usual size therefore takes
 * the full 3 s, which showed as a slow, stepped rise. The System Framework hook
 * ({@link SystemServerHooks}) turns that ramp into a jump; without it the ramp stays. The HBM
 * switch is a separate hardware step, so it waits until the ramp is over: landing inside it showed
 * as a jump followed by a further rise.</p>
 */
final class DozeRamp {
    static final long UNKNOWN = -1L;
    /** screenBrightnessRampFastIncrease / screenBrightnessRampFastDecrease, HLG per second. */
    static final float RAMP_RATE_PER_SECOND = 0.06f;
    /** screenBrightnessRampIncreaseMaxMillis / screenBrightnessRampDecreaseMaxMillis. */
    static final long RAMP_MAX_MS = 3_000L;
    /** The delay the v1.5.x Extra Bright path used after entering AOD. */
    static final long MIN_HBM_SETTLE_MS = 250L;
    static final long HBM_SETTLE_MARGIN_MS = 150L;
    /** Bounds the wait when a large animator duration scale stretches the ramp. */
    static final long MAX_HBM_SETTLE_MS = 10_000L;

    // Android BrightnessUtils HLG constants.
    private static final float HLG_R = 0.5f;
    private static final float HLG_A = 0.17883277f;
    private static final float HLG_B = 0.28466892f;
    private static final float HLG_C = 0.55991073f;

    private DozeRamp() {}

    /** @return how long the system ramps from {@code from} to {@code to}, or {@link #UNKNOWN} */
    static long durationMs(float from, float to, float animatorDurationScale) {
        if (!Float.isFinite(from) || !Float.isFinite(to)) return UNKNOWN;
        float scale = Float.isFinite(animatorDurationScale) ? Math.max(0f, animatorDurationScale) : 1f;
        float seconds = Math.min(Math.abs(hlg(to) - hlg(from)) / RAMP_RATE_PER_SECOND,
                RAMP_MAX_MS / 1000f);
        return Math.round(seconds * scale * 1000f);
    }

    /** Delay from a brightness change to the Extra Bright edge: right after the ramp, never earlier than v1.5.x. */
    static long hbmSettleMs(long rampMs) {
        if (rampMs < 0L) return RAMP_MAX_MS + HBM_SETTLE_MARGIN_MS;
        return Math.max(MIN_HBM_SETTLE_MS, Math.min(MAX_HBM_SETTLE_MS, rampMs + HBM_SETTLE_MARGIN_MS));
    }

    /**
     * @param instant the System Framework hook reported the change as applied at once
     * @return delay from the brightness change until the panel shows the new brightness
     */
    static long settleMs(long rampMs, boolean instant) {
        return instant ? MIN_HBM_SETTLE_MS : hbmSettleMs(rampMs);
    }

    /** Linear display brightness to the perceived (HLG) scale DisplayPowerController ramps in. */
    static float hlg(float linear) {
        float normalized = Math.max(0f, Math.min(1f, linear)) * 12f;
        if (normalized <= 1f) return HLG_R * (float) Math.sqrt(normalized);
        return HLG_A * (float) Math.log(normalized - HLG_B) + HLG_C;
    }
}
