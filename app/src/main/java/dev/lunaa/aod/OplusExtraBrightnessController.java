package dev.lunaa.aod;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Fail-safe bridge to the proven Oplus AOD fingerprint-HBM control path.
 *
 * <p>On lunaa, the kernel only emits the real AOD-HBM command on a logical fingerprint press
 * edge (0 -> 1). SELinux blocks direct writes from the injected SystemUI process, so all
 * notify_fppress writes are executed by the companion app process through its granted root.</p>
 */
public final class OplusExtraBrightnessController {
    private static final String TAG = "LunaaAOD";
    static final long STOCK_UDFPS_HANDOFF_MS = 250L;
    static final long ROOT_ENABLE_RESULT_TIMEOUT_MS = 6_000L;
    static final long ROOT_RESET_RESULT_TIMEOUT_MS = 5_000L;
    static final long LOGICAL_FP_REARM_DELAY_MS = 250L;
    static final long LOGICAL_FP_REARM_GUARD_MS = 800L;

    private final Handler handler;
    private final ExtraBrightnessDimLayer dimLayer;
    private final RootHbmBridgeClient rootBridge;

    private boolean ambientActive;
    private boolean desired;
    private final HbmSessionLatch sessionLatch = new HbmSessionLatch();
    private boolean logicalRearmScheduled;
    private boolean enableScheduled;
    private boolean rootRequestInFlight;
    private boolean cleanupInFlight;
    private long ambientSinceMs = Long.MIN_VALUE;
    private long sessionGeneration;
    private int levelPercent = ExtraBrightnessLevel.DEFAULT_PERCENT;
    private Runnable enableResultWatchdog;
    private Runnable cleanupResultWatchdog;
    private Runnable logicalRearmGuardRelease;

    private final Runnable enableRunner = new Runnable() {
        @Override
        public void run() {
            enableScheduled = false;
            attemptEnable();
        }
    };

    private final Runnable logicalRearmRunner = new Runnable() {
        @Override
        public void run() {
            logicalRearmScheduled = false;
            attemptLogicalFpRearm();
        }
    };

    public OplusExtraBrightnessController(Handler handler, Context context) {
        this(handler, new ExtraBrightnessDimLayer(context), new RootHbmBridgeClient(context, handler));
    }

    OplusExtraBrightnessController(
            Handler handler,
            ExtraBrightnessDimLayer dimLayer,
            RootHbmBridgeClient rootBridge
    ) {
        if (handler == null) throw new IllegalArgumentException("handler is required");
        if (dimLayer == null) throw new IllegalArgumentException("dimLayer is required");
        if (rootBridge == null) throw new IllegalArgumentException("rootBridge is required");
        this.handler = handler;
        this.dimLayer = dimLayer;
        this.rootBridge = rootBridge;
    }

    public void setAmbientActive(boolean active, long nowMs) {
        if (active) {
            if (ambientActive) return;
            sessionGeneration++;
            ambientActive = true;
            cleanupInFlight = false;
            ambientSinceMs = nowMs;
            Log.i(TAG, "extraBright ambient-session-start gen=" + sessionGeneration);
            scheduleEnableIfNeeded(nowMs);
            return;
        }

        if (!ambientActive && !sessionLatch.isLatched() && !rootRequestInFlight
                && !enableScheduled && !cleanupInFlight) {
            dimLayer.hide();
            return;
        }
        finishSessionInternal("ambient-exit");
    }

    public void finishSession() {
        finishSessionInternal("doze-finish");
    }

    public void setDesired(boolean enabled, int percent) {
        levelPercent = ExtraBrightnessLevel.normalize(percent);
        desired = enabled;

        if (sessionLatch.isLatched() && ambientActive) {
            dimLayer.show(levelPercent);
            if (!enabled) {
                Log.i(TAG, "extraBright policy-off deferred until ambient exit (session latched)");
            }
            return;
        }

        if (!enabled) {
            cancelPendingEnable();
            if (rootRequestInFlight) {
                Log.i(TAG, "extraBright policy-off deferred while root edge in flight");
                return;
            }
            dimLayer.hide();
            return;
        }

        scheduleEnableIfNeeded(android.os.SystemClock.elapsedRealtime());
    }

    public void reassertIfDesired() {
        if (!desired || !ambientActive) return;
        if (sessionLatch.isLatched()) {
            dimLayer.show(levelPercent);
            return;
        }
        scheduleEnableIfNeeded(android.os.SystemClock.elapsedRealtime());
    }

    public boolean onStockBrightnessReset() {
        boolean invalidated = sessionLatch.onStockReset();
        if (!invalidated) {
            if (sessionLatch.isLogicalRearmActive()) {
                Log.i(TAG, "extraBright stock reset ignored during module logical FP rearm");
            }
            return false;
        }
        cancelLogicalFpRearm();
        dimLayer.hide();
        Log.i(TAG, "extraBright stock UDFPS reset invalidated HBM latch; recovery edge deferred");
        return true;
    }

    public void forceOff() {
        desired = false;
        if (ambientActive && sessionLatch.isLatched()) {
            dimLayer.show(levelPercent);
            Log.i(TAG, "extraBright force-off deferred until ambient exit (session latched)");
            return;
        }
        cancelPendingEnable();
        if (!rootRequestInFlight && !cleanupInFlight) {
            dimLayer.hide();
        }
    }

    public boolean isDesired() { return desired; }
    public boolean isSessionLatched() { return sessionLatch.isLatched(); }

    private void scheduleEnableIfNeeded(long nowMs) {
        if (!desired || !ambientActive || sessionLatch.isLatched() || enableScheduled
                || rootRequestInFlight || cleanupInFlight) return;
        long earliest = ambientSinceMs == Long.MIN_VALUE
                ? nowMs : ambientSinceMs + STOCK_UDFPS_HANDOFF_MS;
        long delay = Math.max(0L, earliest - nowMs);
        enableScheduled = true;
        handler.postDelayed(enableRunner, delay);
        Log.i(TAG, "extraBright enable-pending delayMs=" + delay
                + " level=" + levelPercent + "% gen=" + sessionGeneration);
    }

    private void attemptEnable() {
        if (!desired || !ambientActive || sessionLatch.isLatched() || rootRequestInFlight || cleanupInFlight) return;

        // Important Oplus ordering: the proven v1.5.x path enters AOD-HBM first.
        // Updating the dim layer before notify_fppress creates an atomic backlight race
        // on some kernels (panel->is_hbm_enabled). Apply the visual layer only after
        // the HBM edge has been accepted and the session is latched.
        attemptEnableAfterDimSettle();
        return;
    }

    private void attemptEnableAfterDimSettle() {
        if (!desired || !ambientActive || sessionLatch.isLatched() || rootRequestInFlight) return;

        final long requestGeneration = sessionGeneration;
        final int requestLevelPercent = levelPercent;
        rootRequestInFlight = true;
        boolean dispatched = rootBridge.requestEnableEdge((success, detail) -> {
            if (requestGeneration != sessionGeneration) {
                Log.i(TAG, "extraBright stale rootBridge result ignored gen=" + requestGeneration
                        + " current=" + sessionGeneration + " detail=" + detail);
                return;
            }
            cancelEnableResultWatchdog();
            rootRequestInFlight = false;
            if (sessionLatch.isLatched()) return;
            if (success && ambientActive) {
                sessionLatch.markLatched();
                Log.i(TAG, "extraBright session-latched level=" + requestLevelPercent
                        + "% edge=0->1 via=app-root gen=" + requestGeneration
                        + " detail=" + detail);
                dimLayer.show(levelPercent);
                scheduleLogicalFpRearm(requestGeneration);
                if (!desired) {
                    Log.i(TAG, "extraBright policy-off deferred until ambient exit (edge completed)");
                }
                return;
            }
            if (!success) {
                dimLayer.hide();
                Log.w(TAG, "Extra Bright unavailable; root bridge failed detail=" + detail);
            }
        });
        if (!dispatched) {
            rootRequestInFlight = false;
            dimLayer.hide();
            Log.w(TAG, "Extra Bright unavailable; root bridge could not be dispatched");
        } else {
            scheduleEnableResultWatchdog(requestGeneration);
        }
    }

    private void scheduleLogicalFpRearm(long requestGeneration) {
        cancelLogicalFpRearm();
        logicalRearmScheduled = true;
        handler.postDelayed(logicalRearmRunner, LOGICAL_FP_REARM_DELAY_MS);
        Log.i(TAG, "extraBright logical FP rearm pending delayMs=" + LOGICAL_FP_REARM_DELAY_MS
                + " gen=" + requestGeneration);
    }

    private void attemptLogicalFpRearm() {
        if (!ambientActive || !sessionLatch.isLatched() || cleanupInFlight) return;
        final long requestGeneration = sessionGeneration;
        sessionLatch.beginLogicalRearm();
        boolean dispatched = rootBridge.requestReset((success, detail) -> {
            if (requestGeneration != sessionGeneration) return;
            if (!success) {
                sessionLatch.endLogicalRearm();
                Log.w(TAG, "extraBright logical FP rearm failed detail=" + detail);
                return;
            }
            scheduleLogicalRearmGuardRelease(requestGeneration);
            Log.i(TAG, "extraBright logical FP rearmed; physical HBM latch retained detail=" + detail);
        });
        if (!dispatched) {
            sessionLatch.endLogicalRearm();
            Log.w(TAG, "extraBright logical FP rearm dispatch failed");
        }
    }

    private void scheduleLogicalRearmGuardRelease(long requestGeneration) {
        cancelLogicalRearmGuardRelease();
        logicalRearmGuardRelease = () -> {
            logicalRearmGuardRelease = null;
            if (requestGeneration != sessionGeneration) return;
            sessionLatch.endLogicalRearm();
            Log.i(TAG, "extraBright logical FP rearm guard released gen=" + requestGeneration);
        };
        handler.postDelayed(logicalRearmGuardRelease, LOGICAL_FP_REARM_GUARD_MS);
    }

    private void cancelLogicalRearmGuardRelease() {
        if (logicalRearmGuardRelease == null) return;
        handler.removeCallbacks(logicalRearmGuardRelease);
        logicalRearmGuardRelease = null;
    }

    private void cancelLogicalFpRearm() {
        if (logicalRearmScheduled) {
            logicalRearmScheduled = false;
            handler.removeCallbacks(logicalRearmRunner);
        }
        cancelLogicalRearmGuardRelease();
    }

    private void finishSessionInternal(String reason) {
        if (!ambientActive && cleanupInFlight) {
            Log.i(TAG, "extraBright cleanup already in flight reason=" + reason);
            return;
        }

        boolean shouldReset = sessionLatch.isLatched() || rootRequestInFlight || sessionLatch.isLogicalRearmActive();
        cancelEnableResultWatchdog();
        cancelCleanupResultWatchdog();
        cancelLogicalFpRearm();
        sessionGeneration++;
        final long closingGeneration = sessionGeneration;
        ambientActive = false;
        desired = false;
        sessionLatch.clear();
        rootRequestInFlight = false;
        ambientSinceMs = Long.MIN_VALUE;
        cancelPendingEnable();

        if (!shouldReset) {
            cancelCleanupResultWatchdog();
            cleanupInFlight = false;
            dimLayer.hide();
            Log.i(TAG, "extraBright ambient-session-end reason=" + reason
                    + " gen=" + closingGeneration + " reset=false");
            return;
        }

        cleanupInFlight = true;
        boolean dispatched = rootBridge.requestReset((success, detail) -> {
            if (closingGeneration != sessionGeneration || ambientActive) {
                Log.i(TAG, "extraBright stale cleanup result ignored gen=" + closingGeneration
                        + " current=" + sessionGeneration + " detail=" + detail);
                return;
            }
            if (success) {
                cancelCleanupResultWatchdog();
                cleanupInFlight = false;
                dimLayer.hide();
                Log.i(TAG, "extraBright ambient-session-end reason=" + reason
                        + " gen=" + closingGeneration
                        + " reset=true detail=" + detail);
                return;
            }
            Log.w(TAG, "extraBright cleanup reset failed; protective dim layer retained for stock exit"
                    + " reason=" + reason + " detail=" + detail);
            scheduleCleanupResultWatchdog(closingGeneration, reason + "-reset-failed");
        });
        if (!dispatched) {
            Log.w(TAG, "extraBright cleanup reset dispatch failed; protective dim layer retained"
                    + " reason=" + reason);
            scheduleCleanupResultWatchdog(closingGeneration, reason + "-dispatch-failed");
        } else {
            scheduleCleanupResultWatchdog(closingGeneration, reason);
        }
    }

    private void scheduleEnableResultWatchdog(long requestGeneration) {
        cancelEnableResultWatchdog();
        enableResultWatchdog = () -> {
            enableResultWatchdog = null;
            if (requestGeneration != sessionGeneration || !rootRequestInFlight) return;
            if (!ambientActive) return;
            Log.w(TAG, "extraBright rootBridge result still pending; keeping request in flight");
        };
        handler.postDelayed(enableResultWatchdog, ROOT_ENABLE_RESULT_TIMEOUT_MS);
    }

    private void cancelEnableResultWatchdog() {
        if (enableResultWatchdog == null) return;
        handler.removeCallbacks(enableResultWatchdog);
        enableResultWatchdog = null;
    }

    private void scheduleCleanupResultWatchdog(long closingGeneration, String reason) {
        cancelCleanupResultWatchdog();
        cleanupResultWatchdog = () -> {
            cleanupResultWatchdog = null;
            if (closingGeneration != sessionGeneration || ambientActive || !cleanupInFlight) return;
            cleanupInFlight = false;
            dimLayer.hide();
            Log.w(TAG, "extraBright cleanup rootBridge timeout; stock display exit assumed reason=" + reason);
        };
        handler.postDelayed(cleanupResultWatchdog, ROOT_RESET_RESULT_TIMEOUT_MS);
    }

    private void cancelCleanupResultWatchdog() {
        if (cleanupResultWatchdog == null) return;
        handler.removeCallbacks(cleanupResultWatchdog);
        cleanupResultWatchdog = null;
    }

    private void cancelPendingEnable() {
        if (enableScheduled) {
            enableScheduled = false;
            handler.removeCallbacks(enableRunner);
        }
    }
}
