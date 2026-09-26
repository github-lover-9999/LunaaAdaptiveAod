package dev.lunaa.aod;

/**
 * Rules for doze brightness changes without the display ramp, shared by the System Framework hook
 * and SystemUI.
 *
 * <p>The hook gives DisplayPowerController a ramp rate of 0 (a jump) for brightness changes while
 * the display power policy is doze and the module is on. After each such change it stamps the time
 * into a global setting, so SystemUI knows the AOD brightness is already on the panel and Extra
 * Bright need not wait for a ramp.</p>
 */
final class InstantDozeRamp {
    /** Settings.Global key holding the elapsed realtime of the latest doze change applied at once. */
    static final String SETTING = "lunaa_aod_instant_doze_ramp";
    static final long NO_STAMP = -1L;
    /** DisplayManagerInternal.DisplayPowerRequest.POLICY_DOZE */
    static final int POLICY_DOZE = 1;
    static final int UNKNOWN_POLICY = -1;

    private InstantDozeRamp() {}

    /** @return whether the doze brightness change of a request with this policy lands at once */
    static boolean applies(int policy, boolean moduleEnabled) {
        return moduleEnabled && policy == POLICY_DOZE;
    }

    /** @return the rate to pass on to the ramp animator: 0 (a jump) for doze changes */
    static float rampRate(int policy, float rate, boolean moduleEnabled) {
        return applies(policy, moduleEnabled) && rate > 0f ? 0f : rate;
    }

    /**
     * @param stampMs the setting's value; one from an earlier boot is either older than the
     *                session or later than now
     * @return a doze change since {@code sinceMs} was applied at once
     */
    static boolean confirmedSince(long stampMs, long sinceMs, long nowMs) {
        return sinceMs != Long.MIN_VALUE && stampMs != NO_STAMP && stampMs >= sinceMs && stampMs <= nowMs;
    }
}
