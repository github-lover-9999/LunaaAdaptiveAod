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

    @Test
    public void resetWritesOnlyWhileTheModuleMayHoldItsPress() {
        RootCommandGate gate = new RootCommandGate();
        assertTrue("unknown after the process starts", gate.resetNeedsWrite());

        gate.recordWrite("0", true); // an edge began, and a reset superseded it before its 1
        assertFalse("already released: a 0 now could only cancel a real finger press", gate.resetNeedsWrite());

        gate.recordWrite("1", true);
        assertTrue(gate.resetNeedsWrite());

        gate.recordWrite("0", true);
        assertFalse(gate.resetNeedsWrite());
    }

    @Test
    public void failedOrTimedOutWriteLeavesThePressUnknown() {
        RootCommandGate gate = new RootCommandGate();
        gate.recordWrite("0", true);
        gate.recordWrite("1", false);
        assertTrue(gate.resetNeedsWrite());
    }
}
