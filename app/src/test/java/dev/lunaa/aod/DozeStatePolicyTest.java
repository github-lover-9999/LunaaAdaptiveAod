package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DozeStatePolicyTest {
    @Test public void recognizesOnlyAmbientIntentStates() {
        assertTrue(DozeStatePolicy.isAmbientIntentState("DOZE"));
        assertTrue(DozeStatePolicy.isAmbientIntentState("DOZE_AOD"));
        assertTrue(DozeStatePolicy.isAmbientIntentState("DOZE_AOD_DOCKED"));
        assertTrue(DozeStatePolicy.isAmbientIntentState("DOZE_AOD_MINMODE"));
        assertTrue(DozeStatePolicy.isAmbientIntentState("DOZE_AOD_PAUSING"));
        assertTrue(DozeStatePolicy.isAmbientIntentState("DOZE_AOD_PAUSED"));

        assertFalse(DozeStatePolicy.isAmbientIntentState("FINISH"));
        assertFalse(DozeStatePolicy.isAmbientIntentState("DOZE_PULSING_BRIGHT"));
        assertFalse(DozeStatePolicy.isAmbientIntentState("INITIALIZED"));
        assertFalse(DozeStatePolicy.isAmbientIntentState(null));
    }
}
