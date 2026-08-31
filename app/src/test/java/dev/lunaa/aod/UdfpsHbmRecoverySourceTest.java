package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UdfpsHbmRecoverySourceTest {
    @Test
    public void syntheticHbmEdgeIsLogicallyRearmedWithoutDroppingProtectiveLayer() throws Exception {
        String controller = read("OplusExtraBrightnessController.java");
        assertTrue(controller.contains("LOGICAL_FP_REARM_DELAY_MS"));
        assertTrue(controller.contains("scheduleLogicalFpRearm"));
        assertTrue(controller.contains("sessionLatch.beginLogicalRearm()"));
        assertTrue(controller.contains("rootBridge.requestReset"));
        assertTrue(controller.contains("sessionLatch.endLogicalRearm()"));
        assertTrue(controller.contains("logical FP rearmed"));
        assertFalse(controller.contains("logical FP rearmed; dim layer hidden"));
    }

    @Test
    public void stockUdfpsResetInvalidatesLatchAndReassertsOnlyAfterRecoveryDelay() throws Exception {
        String controller = read("OplusExtraBrightnessController.java");
        String adaptive = read("AdaptiveAodController.java");

        assertTrue(controller.contains("public boolean onStockBrightnessReset()"));
        assertTrue(controller.contains("sessionLatch.onStockReset()"));
        assertTrue(adaptive.contains("extraBrightnessController.onStockBrightnessReset()"));

        int resetStart = adaptive.indexOf("public void reapplyAfterReset()");
        int resetEnd = adaptive.indexOf("public void destroy()", resetStart);
        String resetMethod = adaptive.substring(resetStart, resetEnd);
        assertTrue(resetMethod.contains("reapplyBrightnessTarget(\"after-stock-reset\")"));
        assertTrue(resetMethod.contains("handler.postDelayed(udfpsRecoveryRunner, UDFPS_RECOVERY_REAPPLY_MS)"));
        assertFalse("stock reset must not immediately emit a new synthetic FP edge",
                resetMethod.contains("extraBrightnessController.reassertIfDesired()"));

        int runnerStart = adaptive.indexOf("private final Runnable udfpsRecoveryRunner");
        int runnerEnd = adaptive.indexOf("private final Runnable extraBrightnessEvaluationRunner", runnerStart);
        String runner = adaptive.substring(runnerStart, runnerEnd);
        assertTrue(runner.contains("reapplyCurrentTarget(\"udfps-recovery\")"));
    }

    private static String read(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
