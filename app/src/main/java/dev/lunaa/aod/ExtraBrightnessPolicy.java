package dev.lunaa.aod;

/** Pure policy for the optional vendor AOD-HBM boost. */
public final class ExtraBrightnessPolicy {
    public static final float ENABLE_LUX = 1_500f;
    public static final float DISABLE_LUX = 700f;
    public static final long ENABLE_DWELL_MS = 600L;
    public static final long DISABLE_DWELL_MS = 1_500L;

    private boolean desired;
    private long enableCandidateSinceMs = Long.MIN_VALUE;
    private long disableCandidateSinceMs = Long.MIN_VALUE;

    public boolean update(long nowMs, float lux, AodSettingsSnapshot settings, boolean ambientActive) {
        return update(nowMs, lux, Long.MAX_VALUE, settings, ambientActive);
    }

    /**
     * @param brightSinceMs since when the light has been at least {@link #ENABLE_LUX}, before AOD
     *                      too; the enable dwell counts from there. {@link Long#MAX_VALUE} when
     *                      unknown.
     */
    public boolean update(long nowMs, float lux, long brightSinceMs, AodSettingsSnapshot settings,
            boolean ambientActive) {
        if (!isBaseEligible(settings, ambientActive)) {
            reset();
            return false;
        }

        if (settings.isManualMode()) {
            desired = settings.getManualLevel() == BrightnessLevelConfig.MAX_LEVEL
                    && settings.isManualExtraBrightEnabled();
            resetCandidates();
            return desired;
        }

        if (!settings.isAutomaticMode()
                || settings.getPreset() != AodPreset.BRIGHT
                || !Float.isFinite(lux)
                || lux < 0f) {
            reset();
            return false;
        }

        if (!desired) {
            disableCandidateSinceMs = Long.MIN_VALUE;
            if (lux >= ENABLE_LUX) {
                if (enableCandidateSinceMs == Long.MIN_VALUE || nowMs < enableCandidateSinceMs) {
                    enableCandidateSinceMs = nowMs;
                }
                enableCandidateSinceMs = Math.min(enableCandidateSinceMs, brightSinceMs);
                if (nowMs - enableCandidateSinceMs >= ENABLE_DWELL_MS) {
                    desired = true;
                    enableCandidateSinceMs = Long.MIN_VALUE;
                }
            } else {
                enableCandidateSinceMs = Long.MIN_VALUE;
            }
            return desired;
        }

        enableCandidateSinceMs = Long.MIN_VALUE;
        if (lux < DISABLE_LUX) {
            if (disableCandidateSinceMs == Long.MIN_VALUE || nowMs < disableCandidateSinceMs) {
                disableCandidateSinceMs = nowMs;
            }
            if (nowMs - disableCandidateSinceMs >= DISABLE_DWELL_MS) {
                desired = false;
                disableCandidateSinceMs = Long.MIN_VALUE;
            }
        } else {
            disableCandidateSinceMs = Long.MIN_VALUE;
        }
        return desired;
    }

    public boolean isDesired() { return desired; }

    public void reset() {
        desired = false;
        resetCandidates();
    }

    private void resetCandidates() {
        enableCandidateSinceMs = Long.MIN_VALUE;
        disableCandidateSinceMs = Long.MIN_VALUE;
    }

    private static boolean isBaseEligible(AodSettingsSnapshot settings, boolean ambientActive) {
        return ambientActive && settings != null && settings.isEnabled();
    }
}
