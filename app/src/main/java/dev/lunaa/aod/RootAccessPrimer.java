package dev.lunaa.aod;

import android.content.Context;
import android.util.Log;

final class RootAccessPrimer {
    private static final String TAG = "LunaaAODRoot";

    private RootAccessPrimer() {}

    static void request(Context context) {
        if (context == null) return;
        Thread worker = new Thread(() -> {
            try {
                java.lang.Process process = new ProcessBuilder("su", "-c", "id")
                        .redirectErrorStream(true)
                        .start();
                int exit = process.waitFor();
                Log.i(TAG, "root prime exit=" + exit);
            } catch (Throwable t) {
                Log.w(TAG, "root prime failed", t);
            }
        }, "LunaaAodRootPrime");
        worker.start();
    }
}
