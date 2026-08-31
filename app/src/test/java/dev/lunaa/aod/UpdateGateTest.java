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
}
