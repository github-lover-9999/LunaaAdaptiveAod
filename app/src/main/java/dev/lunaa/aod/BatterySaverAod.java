package dev.lunaa.aod;

/**
 * "Keep AOD on in Battery Saver": Battery Saver answers Android's question whether AOD must stay
 * off (PowerManager.ServiceType.AOD, policy entry {@code disable_aod}). With the option on the
 * module answers no, and every other Battery Saver limit stays as it is. SystemUI asks again each
 * time Battery Saver turns on or off.
 */
final class BatterySaverAod {
    /** android.os.PowerManager.ServiceType.AOD */
    static final int SERVICE_TYPE_AOD = 14;

    private BatterySaverAod() {}

    /**
     * @param aodBlocked Battery Saver's own answer: AOD must stay off
     * @param keepAod    the module option
     * @return the answer for AOD becomes "not blocked"
     */
    static boolean overrides(int serviceType, boolean aodBlocked, boolean keepAod) {
        return keepAod && aodBlocked && serviceType == SERVICE_TYPE_AOD;
    }
}
