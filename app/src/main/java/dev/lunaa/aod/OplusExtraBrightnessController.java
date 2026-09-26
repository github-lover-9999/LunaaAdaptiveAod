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
    /**
     * After a pulse (a fingerprint touch or a notification) the user often touches the sensor
     * again within a few seconds. The synthetic HBM edge must not land in that retry.
     */
    static final long STOCK_FINGERPRINT_RETRY_GRACE_MS = 4_000L;
    /**
     * Shortest time the dim layer stays after a pulse took the panel out of AOD with Extra Bright
     * on. During a fingerprint touch the kernel sizes its own dim layer from the backlight, so the
     * layer stays until the backlight has come down from the AOD level to the handback level: at
     * once with the System Framework hook, after the display ramp (up to 3 s) without it. Until
     * then the fingerprint HBM would show the screen brighter than the chosen Extra Bright strength.
     */
    static final long PULSE_DIM_MIN_HOLD_MS = 300L;
    /** Keeps the CPU awake past a scheduled edge: the root writes, the rearm and its guard. */
    static final long EDGE_WAKE_MARGIN_MS = 2_000L;
    static final long ROOT_ENABLE_RESULT_TIMEOUT_MS = 6_000L;
    static final long ROOT_RESET_RESULT_TIMEOUT_MS = 5_000L;
    static final long LOGICAL_FP_REARM_DELAY_MS = 250L;
    static final long LOGICAL_FP_REARM_GUARD_MS = 800L;

    private final Handler handler;
    private final ExtraBrightnessDimLayer dimLayer;
    private final RootHbmBridgeClient rootBridge;
    private final AodWakeLock wakeLock;
    private final InstantRampProbe rampProbe;

    private boolean ambientActive;
    private boolean desired;
    private final HbmSessionLatch sessionLatch = new HbmSessionLatch();
    private boolean logicalRearmScheduled;
    private boolean enableScheduled;
    private boolean rootRequestInFlight;
    private boolean cleanupInFlight;
    private long ambientSinceMs = Long.MIN_VALUE;
    /** How long the system ramps to the AOD brightness when it does not jump there. */
    private long entryRampMs = DozeRamp.UNKNOWN;
    private long handbackAtMs = Long.MIN_VALUE;
    private long handbackRampMs = DozeRamp.UNKNOWN;
    private boolean pulseDimHeld;
    private boolean followsStockFingerprint;
    private long retryGraceUntilMs = Long.MIN_VALUE;
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

    private final Runnable pulseDimRelease = () -> endPulseDimHold("timeout");

    public OplusExtraBrightnessController(Handler handler, Context context) {
        this(handler, new ExtraBrightnessDimLayer(context), new RootHbmBridgeClient(context, handler),
                new AodWakeLock(context, "LunaaAOD:hbm"), new InstantRampProbe(context));
    }

    OplusExtraBrightnessController(
            Handler handler,
            ExtraBrightnessDimLayer dimLayer,
            RootHbmBridgeClient rootBridge,
            AodWakeLock wakeLock,
            InstantRampProbe rampProbe
    ) {
        if (handler == null) throw new IllegalArgumentException("handler is required");
        if (dimLayer == null) throw new IllegalArgumentException("dimLayer is required");
        if (rootBridge == null) throw new IllegalArgumentException("rootBridge is required");
        if (wakeLock == null) throw new IllegalArgumentException("wakeLock is required");
        if (rampProbe == null) throw new IllegalArgumentException("rampProbe is required");
        this.handler = handler;
        this.dimLayer = dimLayer;
        this.rootBridge = rootBridge;
        this.wakeLock = wakeLock;
        this.rampProbe = rampProbe;
    }

    public void setAmbientActive(boolean active, long nowMs) {
        if (active) {
            if (ambientActive) return;
            endPulseDimHold("aod-resumed");
            sessionGeneration++;
            ambientActive = true;
            cleanupInFlight = false;
            ambientSinceMs = nowMs;
            entryRampMs = DozeRamp.UNKNOWN;
            retryGraceUntilMs = followsStockFingerprint
                    ? nowMs + STOCK_FINGERPRINT_RETRY_GRACE_MS : Long.MIN_VALUE;
            followsStockFingerprint = false;
            Log.i(TAG, "extraBright ambient-session-start gen=" + sessionGeneration);
            scheduleEnableIfNeeded(nowMs);
            return;
        }

        if (!ambientActive && !sessionLatch.isLatched() && !rootRequestInFlight
                && !enableScheduled && !cleanupInFlight) {
            hideDim();
            return;
        }
        finishSessionInternal("ambient-exit", true);
    }

    public void finishSession() {
        endPulseDimHold("doze-finish");
        finishSessionInternal("doze-finish", false);
    }

    /**
     * Without the System Framework hook the display ramps to the AOD brightness for about
     * {@code rampMs}; the edge then follows the ramp.
     */
    public void setEntryRampMs(long rampMs) {
        entryRampMs = rampMs;
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
            hideDim();
            return;
        }

        scheduleEnableIfNeeded(android.os.SystemClock.elapsedRealtime());
    }

    public void reassertIfDesired() {
        if (!desired || !ambientActive || sessionLatch.isYieldedToStockFingerprint()) return;
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
        hideDim();
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
            hideDim();
        }
    }

    /**
     * SystemUI is taking the panel out of AOD (UDFPS touch, pulse or wake-up), so the stock
     * fingerprint path now owns notify_fppress. Issue no further synthetic edge or rearm, and
     * release a synthetic press that may still be held before the real finger-down edge lands.
     * The protective dim layer stays until the display actually leaves AOD.
     *
     * @param handbackRampMs how long the system ramps down to the handback brightness when it
     *                       does not jump there
     */
    public void yieldToStockFingerprint(long handbackRampMs) {
        if (sessionLatch.isYieldedToStockFingerprint()) return;
        handbackAtMs = android.os.SystemClock.elapsedRealtime();
        this.handbackRampMs = handbackRampMs;
        sessionLatch.yieldToStockFingerprint();
        followsStockFingerprint = true;
        desired = false;
        cancelPendingEnable();
        cancelLogicalFpRearm();
        boolean release = ambientActive && needsLogicalRelease();
        Log.i(TAG, "extraBright yielded notify_fppress to stock fingerprint release=" + release
                + " gen=" + sessionGeneration);
        if (release) releaseLogicalPressForStock();
    }

    /** SystemUI is back in AOD at low power: the module may drive notify_fppress again. */
    public void resumeAfterStockFingerprint(long nowMs) {
        if (!sessionLatch.isYieldedToStockFingerprint()) return;
        sessionLatch.resumeFromStockFingerprint();
        if (ambientActive) {
            ambientSinceMs = nowMs;
            retryGraceUntilMs = nowMs + STOCK_FINGERPRINT_RETRY_GRACE_MS;
            followsStockFingerprint = false;
        }
        Log.i(TAG, "extraBright notify_fppress resumed after stock fingerprint gen=" + sessionGeneration);
        scheduleEnableIfNeeded(nowMs);
    }

    public boolean isDesired() { return desired; }
    public boolean isSessionLatched() { return sessionLatch.isLatched(); }

    /** The kernel may still hold our synthetic press, or an edge that could press it is in flight. */
    private boolean needsLogicalRelease() {
        return rootRequestInFlight || sessionLatch.isLogicalPressHeld();
    }

    private void releaseLogicalPressForStock() {
        final long requestGeneration = sessionGeneration;
        boolean dispatched = rootBridge.requestReset((success, detail) -> {
            if (requestGeneration != sessionGeneration) return;
            if (success) {
                sessionLatch.markLogicalPressReleased();
                Log.i(TAG, "extraBright logical FP released for stock fingerprint detail=" + detail);
            } else {
                Log.w(TAG, "extraBright logical FP release for stock fingerprint failed detail=" + detail);
            }
        });
        if (!dispatched) {
            Log.w(TAG, "extraBright logical FP release for stock fingerprint dispatch failed");
        }
    }

    /** The first try comes soon after entering AOD; it waits longer only for a system ramp. */
    private void scheduleEnableIfNeeded(long nowMs) {
        if (!desired || !ambientActive || sessionLatch.isLatched() || enableScheduled
                || rootRequestInFlight || cleanupInFlight
                || sessionLatch.isYieldedToStockFingerprint()) return;
        long earliest = ambientSinceMs == Long.MIN_VALUE
                ? nowMs : Math.max(ambientSinceMs + DozeRamp.MIN_HBM_SETTLE_MS, retryGraceUntilMs);
        postEnable(Math.max(0L, earliest - nowMs));
        Log.i(TAG, "extraBright enable-pending delayMs=" + Math.max(0L, earliest - nowMs)
                + " level=" + levelPercent + "% gen=" + sessionGeneration);
    }

    private void postEnable(long delayMs) {
        enableScheduled = true;
        handler.postDelayed(enableRunner, delayMs);
        wakeLock.holdFor(delayMs + EDGE_WAKE_MARGIN_MS);
    }

    /** Time left until the panel shows the AOD brightness: none once the jump is confirmed. */
    private long remainingEntryRampMs(long nowMs) {
        if (ambientSinceMs == Long.MIN_VALUE) return 0L;
        boolean instant = rampProbe.instantSince(ambientSinceMs, nowMs);
        return ambientSinceMs + DozeRamp.settleMs(entryRampMs, instant) - nowMs;
    }

    private void attemptEnable() {
        if (!desired || !ambientActive || sessionLatch.isLatched() || rootRequestInFlight || cleanupInFlight
                || sessionLatch.isYieldedToStockFingerprint()) return;
        long waitMs = remainingEntryRampMs(android.os.SystemClock.elapsedRealtime());
        if (waitMs > 0L) {
            postEnable(waitMs);
            Log.i(TAG, "extraBright waits for the system AOD brightness ramp delayMs=" + waitMs
                    + " gen=" + sessionGeneration);
            return;
        }

        // Important Oplus ordering: the proven v1.5.x path enters AOD-HBM first.
        // Updating the dim layer before notify_fppress creates an atomic backlight race
        // on some kernels (panel->is_hbm_enabled). Apply the visual layer only after
        // the HBM edge has been accepted and the session is latched.
        attemptEnableAfterDimSettle();
        return;
    }

    private void attemptEnableAfterDimSettle() {
        if (!desired || !ambientActive || sessionLatch.isLatched() || rootRequestInFlight
                || sessionLatch.isYieldedToStockFingerprint()) return;

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
                if (sessionLatch.isYieldedToStockFingerprint()) {
                    Log.i(TAG, "extraBright edge completed after yield; stock release already queued");
                } else {
                    scheduleLogicalFpRearm(requestGeneration);
                }
                if (!desired) {
                    Log.i(TAG, "extraBright policy-off deferred until ambient exit (edge completed)");
                }
                return;
            }
            if (!success) {
                hideDim();
                Log.w(TAG, "Extra Bright unavailable; root bridge failed detail=" + detail);
            }
        });
        if (!dispatched) {
            rootRequestInFlight = false;
            hideDim();
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
        if (!ambientActive || !sessionLatch.isLatched() || cleanupInFlight
                || sessionLatch.isYieldedToStockFingerprint()) return;
        final long requestGeneration = sessionGeneration;
        sessionLatch.beginLogicalRearm();
        boolean dispatched = rootBridge.requestReset((success, detail) -> {
            if (requestGeneration != sessionGeneration) return;
            if (!success) {
                sessionLatch.endLogicalRearm();
                Log.w(TAG, "extraBright logical FP rearm failed detail=" + detail);
                return;
            }
            sessionLatch.markLogicalPressReleased();
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

    /**
     * @param displayLeavingAod after the stock fingerprint path took over (a pulse), keep the dim
     *                          layer until the backlight is down at the handback level
     */
    private void finishSessionInternal(String reason, boolean displayLeavingAod) {
        if (!ambientActive && cleanupInFlight) {
            Log.i(TAG, "extraBright cleanup already in flight reason=" + reason);
            return;
        }

        // Once the stock fingerprint path owns notify_fppress, a reset here could cancel a real
        // finger press, and a press we already released needs no reset: stock display exit owns
        // the physical HBM-off.
        boolean yielded = sessionLatch.isYieldedToStockFingerprint();
        boolean shouldReset = !yielded && needsLogicalRelease();
        boolean holdDim = displayLeavingAod && yielded && dimLayer.isAttached();
        long nowMs = android.os.SystemClock.elapsedRealtime();
        long dimHoldMs = holdDim ? pulseDimHoldMs(nowMs) : 0L;
        wakeLock.release();
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
            if (holdDim) {
                startPulseDimHold(dimHoldMs);
            } else {
                hideDim();
            }
            Log.i(TAG, "extraBright ambient-session-end reason=" + reason
                    + " gen=" + closingGeneration + " reset=false"
                    + (holdDim ? " dimHeldMs=" + dimHoldMs : ""));
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
                hideDim();
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
            hideDim();
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

    /** Until the backlight has reached the handback brightness, and at least {@link #PULSE_DIM_MIN_HOLD_MS}. */
    private long pulseDimHoldMs(long nowMs) {
        if (handbackAtMs == Long.MIN_VALUE) return DozeRamp.hbmSettleMs(DozeRamp.UNKNOWN);
        boolean instant = rampProbe.instantSince(handbackAtMs, nowMs);
        long settledAtMs = handbackAtMs + DozeRamp.settleMs(handbackRampMs, instant);
        return Math.max(PULSE_DIM_MIN_HOLD_MS, settledAtMs - nowMs);
    }

    private void startPulseDimHold(long holdMs) {
        pulseDimHeld = true;
        handler.removeCallbacks(pulseDimRelease);
        handler.postDelayed(pulseDimRelease, holdMs);
    }

    private void endPulseDimHold(String reason) {
        if (!pulseDimHeld) return;
        pulseDimHeld = false;
        handler.removeCallbacks(pulseDimRelease);
        dimLayer.hide();
        Log.i(TAG, "extraBright pulse dim released reason=" + reason);
    }

    /** Hides the dim layer unless it is held through a pulse. */
    private void hideDim() {
        if (pulseDimHeld) return;
        dimLayer.hide();
    }
}
