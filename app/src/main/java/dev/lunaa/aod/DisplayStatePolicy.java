package dev.lunaa.aod;

public final class DisplayStatePolicy {
    // android.view.Display.STATE_DOZE / STATE_DOZE_SUSPEND.
    private static final int STATE_DOZE = 3;
    private static final int STATE_DOZE_SUSPEND = 4;

    private DisplayStatePolicy() {}

    public static boolean isAmbientState(int state) {
        return state == STATE_DOZE || state == STATE_DOZE_SUSPEND;
    }
}
