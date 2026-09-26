package dev.lunaa.aod;

public final class DozeStatePolicy {
    private DozeStatePolicy() {}

    public static boolean isAmbientIntentState(String stateName) {
        if (stateName == null) return false;
        switch (stateName) {
            case "DOZE":
            case "DOZE_AOD":
            case "DOZE_AOD_DOCKED":
            case "DOZE_AOD_MINMODE":
            case "DOZE_AOD_PAUSING":
            case "DOZE_AOD_PAUSED":
                return true;
            default:
                return false;
        }
    }

    /**
     * States in which stock SystemUI drives the panel itself: a pulse (UDFPS touch or
     * notification) turns the display to full power, and FINISH ends doze on wake-up.
     */
    public static boolean isStockOwnedState(String stateName) {
        if (stateName == null) return false;
        switch (stateName) {
            case "DOZE_REQUEST_PULSE":
            case "DOZE_PULSING":
            case "DOZE_PULSING_BRIGHT":
            case "DOZE_PULSING_WITHOUT_UI":
            case "DOZE_PULSING_AUTH_UI":
            case "DOZE_PULSE_DONE":
            case "FINISH":
                return true;
            default:
                return false;
        }
    }

    /**
     * AOD paused because the proximity sensor reports something close: a hand or a pocket covers
     * the sensors, so the light sensor does not read the ambient light.
     */
    public static boolean isSensorCoveredState(String stateName) {
        return "DOZE_AOD_PAUSING".equals(stateName) || "DOZE_AOD_PAUSED".equals(stateName);
    }
}
