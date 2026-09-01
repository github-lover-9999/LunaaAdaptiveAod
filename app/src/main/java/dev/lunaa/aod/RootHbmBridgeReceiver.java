package dev.lunaa.aod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class RootHbmBridgeReceiver extends BroadcastReceiver {
    private static final String TAG = "LunaaAODRoot";
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String CONTROL_PATH = HbmCapabilityProbe.CONTROL_PATH;
    private static final long EDGE_DELAY_MS = 100L;
    private static final long ROOT_WRITE_TIMEOUT_MS = 2_000L;

    private static final int WRITE_FAILED = 0;
    private static final int WRITE_OK = 1;
    private static final int WRITE_TIMEOUT = 2;

    private static final RootCommandGate COMMAND_GATE = new RootCommandGate();
    private static final ExecutorService ROOT_EXECUTOR = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "LunaaAodRootBridge");
                    thread.setDaemon(true);
                    return thread;
                }
            }
    );

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        boolean enableEdge = RootHbmBridgeClient.ACTION_ENABLE_EDGE.equals(action);
        boolean reset = RootHbmBridgeClient.ACTION_RESET.equals(action);
        if (!enableEdge && !reset) {
            setResultCode(RootHbmBridgeClient.RESULT_FAILURE);
            setResultData("bad-action");
            return;
        }

        int senderUid = -1;
        String senderPackage = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            senderUid = getSentFromUid();
            senderPackage = getSentFromPackage();
        } else {
            senderPackage = SYSTEM_UI;
            try {
                senderUid = context.getPackageManager().getPackageUid(SYSTEM_UI, 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Could not resolve SystemUI UID; sender validation will fail");
                senderUid = -1;
            }
        }
        PackageManager packageManager = context.getPackageManager();
        String[] packagesForUid = packageManager == null ? null : packageManager.getPackagesForUid(senderUid);
        boolean systemUiIsSystemApp = false;
        int systemUiFlags = 0;
        if (packageManager != null) {
            try {
                ApplicationInfo info = packageManager.getApplicationInfo(SYSTEM_UI, 0);
                systemUiFlags = info.flags;
                int systemFlags = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
                systemUiIsSystemApp = (info.flags & systemFlags) != 0;
            } catch (PackageManager.NameNotFoundException ignored) {
                systemUiIsSystemApp = false;
            }
        }
        if (!RootBridgeSenderPolicy.isTrusted(
                senderUid, senderPackage, packagesForUid, systemUiIsSystemApp)) {
            Log.w(TAG, "Rejected sender uid=" + senderUid
                    + " package=" + senderPackage
                    + " systemApp=" + systemUiIsSystemApp
                    + " flags=0x" + Integer.toHexString(systemUiFlags));
            setResultCode(RootHbmBridgeClient.RESULT_FAILURE);
            setResultData("sender-rejected");
            return;
        }
        Log.i(TAG, "Accepted SystemUI sender uid=" + senderUid + " package=" + senderPackage);

        final long commandToken = COMMAND_GATE.beginCommand();
        final PendingResult pending = goAsync();
        try {
            ROOT_EXECUTOR.execute(() -> runCommand(pending, commandToken, reset));
        } catch (Throwable t) {
            Log.w(TAG, "Could not schedule root bridge command", t);
            pending.setResultCode(RootHbmBridgeClient.RESULT_FAILURE);
            pending.setResultData("executor-rejected");
            pending.finish();
        }
    }

    private static void runCommand(PendingResult pending, long token, boolean reset) {
        String detail = "root-command-failed";
        int result = RootHbmBridgeClient.RESULT_FAILURE;
        try {
            if (!COMMAND_GATE.isCurrent(token)) {
                detail = "superseded";
            } else {
                HbmCapabilityProbe.Result capability = HbmCapabilityProbe.probeViaRoot();
                if (!capability.supported) {
                    detail = capability.reason;
                    Log.w(TAG, "HBM command rejected by capability gate: " + detail);
                } else if (reset) {
                    int write = runRootWrite("0");
                    if (write == WRITE_OK) {
                        result = RootHbmBridgeClient.RESULT_SUCCESS;
                        detail = "logical-reset";
                        Log.i(TAG, "FP logical reset executed via app root process");
                    } else {
                        detail = write == WRITE_TIMEOUT ? "reset-timeout" : "reset-write-failed";
                    }
                } else {
                    int resetWrite = runRootWrite("0");
                    if (resetWrite != WRITE_OK) {
                        detail = resetWrite == WRITE_TIMEOUT ? "edge-reset-timeout" : "edge-reset-failed";
                    } else {
                        Thread.sleep(EDGE_DELAY_MS);
                        if (!COMMAND_GATE.isCurrent(token)) {
                            detail = "superseded-after-reset";
                        } else {
                            int enableWrite = runRootWrite("1");
                            if (enableWrite == WRITE_OK) {
                                result = RootHbmBridgeClient.RESULT_SUCCESS;
                                detail = "edge-0-to-1";
                                Log.i(TAG, "HBM edge executed via app root process");
                            } else if (enableWrite == WRITE_TIMEOUT) {
                                // The write may have reached the kernel before su stopped responding.
                                // Keep the protective dim layer latched rather than risk raw HBM.
                                result = RootHbmBridgeClient.RESULT_SUCCESS;
                                detail = "edge-state-unknown-protective-latch";
                                Log.w(TAG, "HBM enable write timed out; protective dim layer will remain latched");
                            } else {
                                detail = "edge-enable-failed";
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            detail = "interrupted";
        } catch (Throwable t) {
            detail = t.getClass().getSimpleName();
            Log.w(TAG, "HBM root bridge command failed", t);
        } finally {
            pending.setResultCode(result);
            pending.setResultData(detail);
            pending.finish();
        }
    }

    private static int runRootWrite(String value) {
        java.lang.Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", "echo " + value + " > " + CONTROL_PATH)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(ROOT_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return WRITE_TIMEOUT;
            }
            return process.exitValue() == 0 ? WRITE_OK : WRITE_FAILED;
        } catch (Throwable t) {
            Log.w(TAG, "Root write failed value=" + value, t);
            return WRITE_FAILED;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
