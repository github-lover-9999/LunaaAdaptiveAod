package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.Test;

public class HbmSessionLatchTest {
    @Test
    public void ownLogicalRearmDoesNotInvalidatePhysicalLatchButStockResetDoes() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("dev.lunaa.aod.HbmSessionLatch");
        } catch (ClassNotFoundException missing) {
            fail("HbmSessionLatch production state machine is missing");
            return;
        }

        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object state = constructor.newInstance();
        Method markLatched = method(type, "markLatched");
        Method isLatched = method(type, "isLatched");
        Method beginLogicalRearm = method(type, "beginLogicalRearm");
        Method endLogicalRearm = method(type, "endLogicalRearm");
        Method onStockReset = method(type, "onStockReset");

        assertFalse((Boolean) isLatched.invoke(state));
        markLatched.invoke(state);
        assertTrue((Boolean) isLatched.invoke(state));

        beginLogicalRearm.invoke(state);
        assertFalse("our own logical FP reset must not invalidate physical HBM",
                (Boolean) onStockReset.invoke(state));
        assertTrue((Boolean) isLatched.invoke(state));

        endLogicalRearm.invoke(state);
        assertTrue("a later stock UDFPS reset must invalidate the stale HBM latch",
                (Boolean) onStockReset.invoke(state));
        assertFalse((Boolean) isLatched.invoke(state));
    }

    @Test
    public void syntheticPressIsHeldFromTheEdgeUntilOurLogicalResetReleasesIt() {
        HbmSessionLatch latch = new HbmSessionLatch();
        assertFalse(latch.isLogicalPressHeld());

        latch.markLatched();
        assertTrue("0->1 edge leaves the kernel's logical press at 1", latch.isLogicalPressHeld());

        latch.markLogicalPressReleased();
        assertFalse(latch.isLogicalPressHeld());
        assertTrue("releasing the logical press keeps the physical HBM latch", latch.isLatched());

        latch.markLatched();
        assertTrue("a new edge presses again", latch.isLogicalPressHeld());

        latch.clear();
        assertFalse(latch.isLogicalPressHeld());
    }

    @Test
    public void stockResetThatDropsTheLatchAlsoDropsTheHeldPress() {
        HbmSessionLatch latch = new HbmSessionLatch();
        latch.markLatched();

        assertTrue(latch.onStockReset());
        assertFalse(latch.isLogicalPressHeld());
    }

    @Test
    public void stockFingerprintOwnsTheNodeFromYieldUntilResumeOrSessionEnd() {
        HbmSessionLatch latch = new HbmSessionLatch();
        assertFalse(latch.isYieldedToStockFingerprint());

        latch.yieldToStockFingerprint();
        assertTrue(latch.isYieldedToStockFingerprint());
        latch.resumeFromStockFingerprint();
        assertFalse(latch.isYieldedToStockFingerprint());

        latch.yieldToStockFingerprint();
        latch.clear();
        assertFalse("a new AOD session starts owned by the module", latch.isYieldedToStockFingerprint());
    }

    private static Method method(Class<?> type, String name) throws Exception {
        Method method = type.getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }
}
