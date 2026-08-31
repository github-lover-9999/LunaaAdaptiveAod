package dev.lunaa.aod;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import org.junit.Test;

public class LunaaDevicePolicyTest {
    private boolean supported(String device, String product, String model, String manufacturer) throws Exception {
        Class<?> type = Class.forName("dev.lunaa.aod.LunaaDevicePolicy");
        Method method = type.getDeclaredMethod(
                "isSupportedIdentity", String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(null, device, product, model, manufacturer);
    }

    @Test public void acceptsKnownAndFutureRmx336Variants() throws Exception {
        assertTrue(supported("lunaa", "lineage_lunaa", "RMX3363", "realme"));
        assertTrue(supported("lunaa", "lunaa", "RMX3360", "realme"));
        assertTrue(supported("lunaa", "axion_lunaa", "RMX3365", "realme"));
        assertFalse(supported("unknown", "generic", "RMX3370", "realme"));
    }

    @Test public void acceptsLunaaRealmeIdentityEvenWhenRomRewritesModelName() throws Exception {
        assertTrue(supported("lunaa", "lineage_lunaa", "realme GT Master Edition", "realme"));
        assertFalse(supported("lunaa", "lineage_lunaa", "Pixel 9", "Google"));
    }

    @Test public void acceptsRmx3363EvenWhenCustomRomRewritesDeviceAndProduct() throws Exception {
        assertTrue(supported("qssi", "crdroid_arm64", "RMX3363", "realme"));
        assertTrue(supported("RMX3363", "generic", "RMX3363", "realme"));
        assertTrue(supported("RE54ABL1", "RE54ABL1", "RMX3363", "realme"));
        assertTrue(supported("RE54ABL1", "axion_RE54ABL1", "Pixel 8 Pro", "Google"));
    }


    @Test public void rejectsUnrelatedDevices() throws Exception {
        assertFalse(supported("bitra", "lineage_bitra", "RMX3370", "realme"));
        assertFalse(supported("panther", "panther", "Pixel 7", "Google"));
        assertFalse(supported(null, null, null, null));
    }
}
