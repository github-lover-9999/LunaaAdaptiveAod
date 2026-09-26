package dev.lunaa.aod;
import static org.junit.Assert.*;
import org.junit.Test;
public class UpdateGateTest {
    @Test public void suppressesNoiseButAllowsMeaningfulMovement() {
        UpdateGate gate = new UpdateGate();
        assertTrue(gate.shouldApply(1000, 100f, 0.08f));
        assertFalse(gate.shouldApply(1100, 105f, 0.081f));
        assertTrue(gate.shouldApply(1200, 120f, 0.09f));
        assertFalse(gate.shouldApply(1300, 121f, 0.091f));
        assertTrue(gate.shouldApply(3400, 128f, 0.10f));
    }

    @Test public void stepsTooSmallToSeeAreSkippedNearTheTop() {
        // Each change lands at once, so a step must be visible to be worth it: 0.60 -> 0.62 is
        // 0.006 in the perceived (HLG) scale, 0.60 -> 0.70 is 0.029.
        UpdateGate gate = new UpdateGate();
        assertTrue(gate.shouldApply(1_000L, 2_000f, 0.60f));
        assertFalse(gate.shouldApply(5_000L, 3_000f, 0.62f));
        assertTrue(gate.shouldApply(9_000L, 5_000f, 0.70f));
    }

    @Test public void smallDriftAddsUpUntilItIsVisible() {
        UpdateGate gate = new UpdateGate();
        assertTrue(gate.shouldApply(1_000L, 2_000f, 0.60f));
        assertFalse(gate.shouldApply(4_000L, 2_300f, 0.62f));
        assertFalse("0.009 in the perceived scale", gate.shouldApply(7_000L, 2_600f, 0.63f));
        assertTrue("0.60 -> 0.64 is 0.012 in the perceived scale", gate.shouldApply(10_000L, 3_000f, 0.64f));
    }

    @Test public void theNarrowDimPresetStillFollowsIndoorToDaylight() {
        // Automatic DIM on lunaa: 300 lux 0.1627, 5000 lux 0.1787, 0.019 apart in the perceived scale.
        UpdateGate gate = new UpdateGate();
        assertTrue(gate.shouldApply(1_000L, 300f, 0.1627f));
        assertTrue(gate.shouldApply(5_000L, 5_000f, 0.1787f));
    }
}
