package dev.lunaa.aod;

import android.content.Context;
import android.provider.Settings;

/** SystemUI side of {@link InstantDozeRamp}: reads the stamp the System Framework hook leaves. */
class InstantRampProbe {
    private final Context context;

    InstantRampProbe(Context context) {
        this.context = context;
    }

    /** @return a doze brightness change since {@code sinceMs} landed without the display ramp */
    boolean instantSince(long sinceMs, long nowMs) {
        if (context == null || sinceMs == Long.MIN_VALUE) return false;
        try {
            long stampMs = Settings.Global.getLong(context.getContentResolver(),
                    InstantDozeRamp.SETTING, InstantDozeRamp.NO_STAMP);
            return InstantDozeRamp.confirmedSince(stampMs, sinceMs, nowMs);
        } catch (Throwable t) {
            return false;
        }
    }
}
