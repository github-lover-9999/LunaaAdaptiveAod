package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StartupDipWiringTest {
    @Test public void transitionPreparesIntentBeforeOriginalTransitionRuns() throws Exception {
        String hooks = read("SystemUiHooks.java");
        int transitionStart = hooks.indexOf("\"transitionTo\"");
        int resetStart = hooks.indexOf("\"resetBrightnessToDefault\"");
        String transitionHook = hooks.substring(transitionStart, resetStart);

        assertTrue(transitionHook.contains("Object newState"));
        assertTrue(transitionHook.contains("controller.captureScreenBrightness()"));
        assertTrue(transitionHook.contains("controller.onDozeTransition(newState)"));
        assertFalse(transitionHook.contains("controller.reloadSettings()"));
        assertTrue(transitionHook.indexOf("controller.captureScreenBrightness()")
                < transitionHook.indexOf("controller.onDozeTransition(newState)"));
    }

    @Test public void resetCanUsePreparedTargetWithoutCreatingController() throws Exception {
        String controller = read("AdaptiveAodController.java");
        String hooks = read("SystemUiHooks.java");

        assertTrue(controller.contains("pendingAmbientIntent"));
        assertTrue(controller.contains("engine.prepareAmbientEntry"));
        assertTrue(controller.contains("engine.pendingTarget()"));
        assertTrue(controller.contains("DozeStatePolicy.isAmbientIntentState"));
        assertTrue(controller.contains("public void reapplyAfterReset()"));

        int resetStart = hooks.indexOf("\"resetBrightnessToDefault\"");
        int ensureDefinition = hooks.indexOf("private static AdaptiveAodController ensureController", resetStart);
        String resetHook = hooks.substring(resetStart, ensureDefinition);
        assertTrue(resetHook.contains("controller.reapplyAfterReset()"));
        assertFalse(resetHook.contains("ensureController("));
    }

    @Test public void controllerAppliesPreparedEntryBeforeStockTransitionAndSmoothsLuxChanges() throws Exception {
        String controller = read("AdaptiveAodController.java");

        assertTrue(controller.contains("new BrightnessSmoother()"));
        assertTrue(controller.contains("brightnessTransitionRunner"));
        assertTrue(controller.contains("BrightnessSmoother.FRAME_INTERVAL_MS"));
        assertTrue(controller.contains("applyImmediate(prepared, \"prepare-aod\")"));
        assertTrue(controller.contains("retargetSmooth(target, \"lux\")"));
        assertTrue(controller.contains("smoother.valueAt(nowMs)"));
        assertTrue(controller.contains("stopBrightnessTransition()"));
    }

    @Test public void disabledAndManualPoliciesDoNotKeepAmbientLuxListenerOrWriteStaleTarget() throws Exception {
        String controller = read("AdaptiveAodController.java");

        assertTrue(controller.contains("engine.shouldObserveLux"));
        assertTrue(controller.contains("if (!engine.isEnabled())"));
        assertTrue(controller.contains("unregisterLightSensor()"));
        assertTrue(controller.contains("return;"));
    }

    @Test public void stockResetReappliesCurrentSmoothedBrightnessWithoutRestartingRamp() throws Exception {
        String controller = read("AdaptiveAodController.java");
        int start = controller.indexOf("public void reapplyAfterReset()");
        int end = controller.indexOf("public void destroy()", start);
        String method = controller.substring(start, end);
        int brightnessStart = controller.indexOf("private void reapplyBrightnessTarget");
        int currentStart = controller.indexOf("private void reapplyCurrentTarget", brightnessStart);
        String brightnessHelper = controller.substring(brightnessStart, currentStart);

        assertTrue(method.contains("reapplyBrightnessTarget(\"after-stock-reset\")"));
        assertTrue(brightnessHelper.contains("smoother.valueAt(nowMs)"));
        assertFalse(method.contains("retargetSmooth("));
        assertFalse(method.contains("smoother.retarget("));
        assertFalse(brightnessHelper.contains("retargetSmooth("));
        assertFalse(brightnessHelper.contains("smoother.retarget("));
    }

    private static String read(String file) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + file);
    }
}
