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
        assertTrue(source.contains("lastObservedLux"));
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
        assertTrue(adaptive.contains("lastObservedLuxMs"));
        assertTrue(adaptive.contains("EXTRA_BRIGHTNESS_MAX_LUX_AGE_MS"));
        assertTrue(dim.contains("ExtraBrightnessDimLayer(Context"));
        assertFalse(dim.contains("ActivityThread"));
        assertFalse(dim.contains("currentApplication"));
        assertTrue(dim.contains("keeping existing protective layer"));
    }

    private static String read(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
