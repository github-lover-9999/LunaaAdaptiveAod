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

    private static Method method(Class<?> type, String name) throws Exception {
        Method method = type.getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }
}
