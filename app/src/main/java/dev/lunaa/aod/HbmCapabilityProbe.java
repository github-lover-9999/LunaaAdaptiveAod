package dev.lunaa.aod;

import android.os.Build;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/** Root-side, read-only preflight for the proven Oplus lunaa AOD-HBM path. */
final class HbmCapabilityProbe {
    private static final String TAG = "LunaaAODRoot";
    static final String CONTROL_PATH = "/sys/kernel/oplus_display/notify_fppress";
    static final String DIMLAYER_PATH = "/sys/kernel/oplus_display/dimlayer_hbm";
    static final String PANEL_BRIGHTNESS_PATH = "/sys/class/backlight/panel0-backlight/brightness";
    private static final long PROBE_TIMEOUT_MS = 2_000L;

    private static volatile Result cachedSuccess;

    static Result probeViaRoot() {
        if (!LunaaDevicePolicy.isSupportedIdentity(
                Build.DEVICE, Build.PRODUCT, Build.MODEL, Build.MANUFACTURER)) {
            return Result.failure("unsupported-device-identity");
        }

        Result cached = cachedSuccess;
        if (cached != null) return cached;

        String command = "test -e " + CONTROL_PATH
                + " && test -w " + CONTROL_PATH
                + " && test -e " + DIMLAYER_PATH
                + " && test -r " + DIMLAYER_PATH
                + " && test -e " + PANEL_BRIGHTNESS_PATH
                + " && test -r " + PANEL_BRIGHTNESS_PATH;
        java.lang.Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Result.failure("capability-probe-timeout");
            }
            if (process.exitValue() != 0) {
                return Result.failure("required-oplus-nodes-unavailable");
            }
            Result success = Result.success("lunaa-oplus-aod-hbm");
            cachedSuccess = success;
            Log.i(TAG, "HBM capability confirmed device=" + Build.DEVICE
                    + " product=" + Build.PRODUCT + " model=" + Build.MODEL);
            return success;
        } catch (Throwable t) {
            Log.w(TAG, "HBM capability probe failed", t);
            return Result.failure("capability-probe-failed");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    static final class Result {
        final boolean supported;
        final String reason;

        private Result(boolean supported, String reason) {
            this.supported = supported;
            this.reason = reason;
        }

        static Result success(String reason) { return new Result(true, reason); }
        static Result failure(String reason) { return new Result(false, reason); }
    }
}
