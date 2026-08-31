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
}
