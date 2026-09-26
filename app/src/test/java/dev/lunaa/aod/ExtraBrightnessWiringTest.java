package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExtraBrightnessWiringTest {
    @Test
    public void vendorControllerUsesOnlyProvenOplusFingerprintControlPath() throws Exception {
        String source = read("OplusExtraBrightnessController.java");

        assertTrue(source.contains("RootHbmBridgeClient"));
        assertTrue(source.contains("requestEnableEdge"));
        assertTrue(source.contains("requestReset"));
        assertTrue(source.contains("edge=0->1"));
        assertFalse(source.contains("new ProcessBuilder(\"su\", \"-c\""));
        assertTrue(source.contains("ExtraBrightnessDimLayer"));
        assertTrue(source.contains("HbmSessionLatch"));
        assertTrue(source.contains("sessionLatch.isLatched()"));
        assertFalse(source.contains("REASSERT_INTERVAL_MS"));
        assertFalse(source.contains("/sys/kernel/oplus_display/hbm"));
        assertFalse(source.contains("write_panel_reg"));
        assertFalse(source.contains("dsi_cmd"));
    }

    @Test
    public void adaptiveControllerGatesAndShutsDownExtraBrightnessFailSafe() throws Exception {
        String source = read("AdaptiveAodController.java");

        assertTrue(source.contains("new ExtraBrightnessPolicy()"));
        assertTrue(source.contains("new OplusExtraBrightnessController(handler, systemUiContext)"));
        assertTrue(source.contains("updateExtraBrightness("));
        assertTrue(source.contains("extraBrightnessController.setDesired"));
        assertTrue(source.contains("extraBrightnessController.setAmbientActive"));
        assertTrue(source.contains("extraBrightnessController.forceOff()"));
        assertTrue(source.contains("extraBrightnessController.reassertIfDesired()"));
        assertTrue(source.contains("engine.currentLux("));
        assertFalse(source.contains("UdfpsController"));
        assertFalse(source.contains("OnscreenFingerprint"));
    }


    @Test
    public void enableWatchdogMustNotLatchOrCancelAnInFlightRootRequest() throws Exception {
        String controller = read("OplusExtraBrightnessController.java");
        int start = controller.indexOf("private void scheduleEnableResultWatchdog");
        int end = controller.indexOf("private void cancelEnableResultWatchdog", start);
        assertTrue(start >= 0 && end > start);
        String watchdog = controller.substring(start, end);
        assertFalse(watchdog.contains("sessionLatched = true"));
        assertFalse(watchdog.contains("rootRequestInFlight = false"));
    }

    @Test
    public void extraBrightnessLifecycleIsSessionScopedAndUsesValidatedContext() throws Exception {
        String controller = read("OplusExtraBrightnessController.java");
        String adaptive = read("AdaptiveAodController.java");
        String dim = read("ExtraBrightnessDimLayer.java");

        assertTrue(controller.contains("sessionGeneration"));
        assertTrue(controller.contains("finishSession"));
        assertTrue(controller.contains("cleanupInFlight"));
        assertTrue(controller.contains("policy-off deferred while root edge in flight"));
        assertTrue(controller.contains("if (success && ambientActive)"));
        assertTrue(controller.contains("ROOT_ENABLE_RESULT_TIMEOUT_MS"));
        assertTrue(controller.contains("enableResultWatchdog"));
        assertTrue(controller.contains("ROOT_RESET_RESULT_TIMEOUT_MS"));
        assertTrue(controller.contains("cleanupResultWatchdog"));
        assertTrue(controller.contains("cleanup reset failed; protective dim layer retained for stock exit"));
        assertTrue(adaptive.contains("extraBrightnessController.finishSession()"));
        assertTrue(adaptive.contains("engine.currentLux(nowMs, EXTRA_BRIGHTNESS_MAX_LUX_AGE_MS)"));
        assertTrue(adaptive.contains("engine.setLuxTracking("));
        assertTrue(adaptive.contains("engine.setSensorsCovered(DozeStatePolicy.isSensorCoveredState(stateName),"));
        assertTrue(dim.contains("ExtraBrightnessDimLayer(Context"));
        assertFalse(dim.contains("ActivityThread"));
        assertFalse(dim.contains("currentApplication"));
        assertTrue(dim.contains("keeping existing protective layer"));
    }

    @Test
    public void dimLayerStaysBelowTheUdfpsOverlayAndThroughAFingerprintPulse() throws Exception {
        String dim = read("ExtraBrightnessDimLayer.java");
        String controller = read("OplusExtraBrightnessController.java");

        assertTrue(dim.contains("TYPE_VOLUME_OVERLAY,"));
        assertFalse("not in the UDFPS overlay layer", dim.contains("TYPE_NAVIGATION_BAR_PANEL,"));
        assertTrue(controller.contains("PULSE_DIM_MIN_HOLD_MS"));
        assertTrue(controller.contains("long dimHoldMs = holdDim ? pulseDimHoldMs(nowMs) : 0L;"));
        assertTrue(controller.contains("startPulseDimHold(dimHoldMs)"));
        assertTrue(controller.contains("boolean holdDim = displayLeavingAod && yielded && dimLayer.isAttached();"));
        int hides = controller.split("dimLayer\\.hide\\(\\)", -1).length - 1;
        assertTrue("a held dim layer is hidden only where its hold ends, found " + hides, hides == 2);
        assertTrue(controller.contains("endPulseDimHold(\"aod-resumed\")"));
        assertTrue(controller.contains("endPulseDimHold(\"doze-finish\")"));
    }

    @Test
    public void extraBrightWaitsForTheSystemRampOnlyWhileTheJumpIsUnconfirmed() throws Exception {
        String controller = read("OplusExtraBrightnessController.java");
        String adaptive = read("AdaptiveAodController.java");

        assertTrue(controller.contains("rampProbe.instantSince(ambientSinceMs, nowMs)"));
        assertTrue(controller.contains("DozeRamp.settleMs(entryRampMs, instant)"));
        assertTrue(controller.contains("rampProbe.instantSince(handbackAtMs, nowMs)"));
        assertTrue(controller.contains("DozeRamp.settleMs(handbackRampMs, instant)"));
        assertTrue(controller.contains("new InstantRampProbe(context)"));
        assertTrue(adaptive.contains("extraBrightnessController.yieldToStockFingerprint(handbackRampMs)"));
        assertTrue(adaptive.contains("DozeRamp.durationMs(shownDozeBrightness, handback, ValueAnimator.getDurationScale())"));
    }

    private static String read(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
