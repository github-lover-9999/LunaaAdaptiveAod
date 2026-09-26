package dev.lunaa.aod;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

/**
 * Keeps the CPU awake for short, bounded module work in AOD. While the display shows AOD the CPU
 * suspends between wake-ups and SystemUI's handler stops with it, so a timed step such as the
 * Extra Bright edge would otherwise wait for the next wake-up.
 */
final class AodWakeLock {
    private static final String TAG = "LunaaAOD";

    private final PowerManager.WakeLock wakeLock;

    AodWakeLock(Context context, String tag) {
        PowerManager.WakeLock lock = null;
        try {
            PowerManager powerManager = context == null ? null : context.getSystemService(PowerManager.class);
            if (powerManager != null) {
                lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
                lock.setReferenceCounted(false);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Wake lock unavailable; AOD timing may stretch while the CPU sleeps: " + t);
        }
        wakeLock = lock;
    }

    /** Keeps the CPU awake for at most {@code timeoutMs}; a later call extends it. */
    void holdFor(long timeoutMs) {
        if (wakeLock == null) return;
        try {
            wakeLock.acquire(timeoutMs);
        } catch (Throwable t) {
            Log.w(TAG, "Could not hold the AOD wake lock: " + t);
        }
    }

    void release() {
        if (wakeLock == null) return;
        try {
            if (wakeLock.isHeld()) wakeLock.release();
        } catch (Throwable t) {
            Log.w(TAG, "Could not release the AOD wake lock: " + t);
        }
    }
}
