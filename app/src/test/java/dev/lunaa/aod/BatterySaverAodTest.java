package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BatterySaverAodTest {
    @Test public void aodServiceTypeIsTheOneAndroidUses() {
        // android.os.PowerManager.ServiceType.AOD on Android 16 and 17.
        assertEquals(14, BatterySaverAod.SERVICE_TYPE_AOD);
    }

    @Test public void batterySaverKeepsAodWhenTheOptionIsOn() {
        assertTrue(BatterySaverAod.overrides(BatterySaverAod.SERVICE_TYPE_AOD, true, true));
    }

    @Test public void optionOffLeavesBatterySaverAlone() {
        assertFalse(BatterySaverAod.overrides(BatterySaverAod.SERVICE_TYPE_AOD, true, false));
    }

    @Test public void otherBatterySaverLimitsStay() {
        for (int type = 0; type <= 20; type++) {
            if (type == BatterySaverAod.SERVICE_TYPE_AOD) continue;
            assertFalse("service type " + type, BatterySaverAod.overrides(type, true, true));
        }
    }

    @Test public void nothingToChangeWhenBatterySaverLeavesAodOn() {
        assertFalse(BatterySaverAod.overrides(BatterySaverAod.SERVICE_TYPE_AOD, false, true));
    }
}
