package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RootCommandGateTest {
    @Test
    public void newerCommandSupersedesOlderEnableOrReset() {
        RootCommandGate gate = new RootCommandGate();
        long enable = gate.beginCommand();
        assertTrue(gate.isCurrent(enable));

        long reset = gate.beginCommand();
        assertFalse(gate.isCurrent(enable));
        assertTrue(gate.isCurrent(reset));

        long nextEnable = gate.beginCommand();
        assertFalse(gate.isCurrent(reset));
        assertTrue(gate.isCurrent(nextEnable));
    }
}
