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

    @Test public void pulsesAndFinishHandThePanelToStockSystemUi() {
        assertTrue(DozeStatePolicy.isStockOwnedState("DOZE_REQUEST_PULSE"));
        assertTrue(DozeStatePolicy.isStockOwnedState("DOZE_PULSING"));
        assertTrue(DozeStatePolicy.isStockOwnedState("DOZE_PULSING_BRIGHT"));
        assertTrue(DozeStatePolicy.isStockOwnedState("DOZE_PULSING_WITHOUT_UI"));
        assertTrue(DozeStatePolicy.isStockOwnedState("DOZE_PULSING_AUTH_UI"));
        assertTrue(DozeStatePolicy.isStockOwnedState("DOZE_PULSE_DONE"));
        assertTrue(DozeStatePolicy.isStockOwnedState("FINISH"));

        assertFalse(DozeStatePolicy.isStockOwnedState("DOZE_AOD"));
        assertFalse(DozeStatePolicy.isStockOwnedState("DOZE"));
        assertFalse(DozeStatePolicy.isStockOwnedState("DOZE_AOD_PAUSED"));
        assertFalse(DozeStatePolicy.isStockOwnedState("INITIALIZED"));
        assertFalse(DozeStatePolicy.isStockOwnedState("UNKNOWN_VENDOR_STATE"));
        assertFalse(DozeStatePolicy.isStockOwnedState(null));
    }

    @Test public void proximityPauseMeansTheSensorsAreCovered() {
        assertTrue(DozeStatePolicy.isSensorCoveredState("DOZE_AOD_PAUSING"));
        assertTrue(DozeStatePolicy.isSensorCoveredState("DOZE_AOD_PAUSED"));

        assertFalse(DozeStatePolicy.isSensorCoveredState("DOZE_AOD"));
        assertFalse(DozeStatePolicy.isSensorCoveredState("DOZE"));
        assertFalse(DozeStatePolicy.isSensorCoveredState("DOZE_REQUEST_PULSE"));
        assertFalse(DozeStatePolicy.isSensorCoveredState("FINISH"));
        assertFalse(DozeStatePolicy.isSensorCoveredState(null));
    }
}
