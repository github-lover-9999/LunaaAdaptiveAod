package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InstantDozeRampTest {
    private static final int POLICY_OFF = 0;
    private static final int POLICY_DIM = 2;
    private static final int POLICY_BRIGHT = 3;

    @Test public void dozeBrightnessChangesJumpWhileTheModuleIsOn() {
        assertEquals(0f, InstantDozeRamp.rampRate(InstantDozeRamp.POLICY_DOZE, 0.06f, true), 0f);
        assertEquals("a custom rate too", 0f, InstantDozeRamp.rampRate(InstantDozeRamp.POLICY_DOZE, 0.5f, true), 0f);
        assertEquals(0f, InstantDozeRamp.rampRate(InstantDozeRamp.POLICY_DOZE, 0f, true), 0f);
    }

    @Test public void theScreenOutsideDozeKeepsTheSystemRamp() {
        assertEquals(0.06f, InstantDozeRamp.rampRate(POLICY_OFF, 0.06f, true), 0f);
        assertEquals(0.06f, InstantDozeRamp.rampRate(POLICY_DIM, 0.06f, true), 0f);
        assertEquals("unlock and the normal screen", 0.06f, InstantDozeRamp.rampRate(POLICY_BRIGHT, 0.06f, true), 0f);
        assertEquals(0.06f, InstantDozeRamp.rampRate(InstantDozeRamp.UNKNOWN_POLICY, 0.06f, true), 0f);
    }

    @Test public void disabledModuleKeepsTheStockRamp() {
        assertEquals(0.06f, InstantDozeRamp.rampRate(InstantDozeRamp.POLICY_DOZE, 0.06f, false), 0f);
        assertFalse(InstantDozeRamp.applies(InstantDozeRamp.POLICY_DOZE, false));
    }

    @Test public void onlyDozeChangesAreReportedToSystemUi() {
        assertTrue(InstantDozeRamp.applies(InstantDozeRamp.POLICY_DOZE, true));
        assertFalse(InstantDozeRamp.applies(POLICY_BRIGHT, true));
        assertFalse(InstantDozeRamp.applies(InstantDozeRamp.UNKNOWN_POLICY, true));
    }

    @Test public void aStampFromThisAodSessionConfirmsTheJump() {
        assertTrue(InstantDozeRamp.confirmedSince(10_050L, 10_000L, 10_250L));
        assertTrue(InstantDozeRamp.confirmedSince(10_000L, 10_000L, 10_250L));
        assertTrue(InstantDozeRamp.confirmedSince(10_250L, 10_000L, 10_250L));
    }

    @Test public void olderOrForeignStampsDoNotConfirm() {
        assertFalse("before this session", InstantDozeRamp.confirmedSince(9_990L, 10_000L, 10_250L));
        assertFalse("from an earlier boot with a longer uptime",
                InstantDozeRamp.confirmedSince(900_000L, 10_000L, 10_250L));
        assertFalse("no stamp", InstantDozeRamp.confirmedSince(InstantDozeRamp.NO_STAMP, 10_000L, 10_250L));
        assertFalse("no session", InstantDozeRamp.confirmedSince(10_050L, Long.MIN_VALUE, 10_250L));
    }
}
