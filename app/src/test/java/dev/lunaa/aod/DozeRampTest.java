package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DozeRampTest {
    @Test public void perceivedScaleIsTheHlgScaleTheSystemRampsIn() {
        // Android's BrightnessUtils.convertLinearToGamma (HLG).
        assertEquals(0.0f, DozeRamp.hlg(0.0f), 0.0001f);
        assertEquals(0.5f, DozeRamp.hlg(1f / 12f), 0.0001f);
        assertEquals(0.8716f, DozeRamp.hlg(0.5f), 0.0001f);
        assertEquals(1.0f, DozeRamp.hlg(1.0f), 0.0001f);
    }

    @Test public void lunaaRampsDozeChangesAtItsDisplayConfigRate() {
        // screenBrightnessRampFastIncrease/Decrease 0.06 per second.
        // HLG(0.30) = 0.7743, HLG(0.35) = 0.8040: 0.0297 at 0.06 per second is ~496 ms.
        assertEquals(496L, DozeRamp.durationMs(0.30f, 0.35f, 1f), 3L);
        assertEquals(496L, DozeRamp.durationMs(0.35f, 0.30f, 1f), 3L);
    }

    @Test public void longRampsAreCutToTheConfiguredMaximum() {
        // screenBrightnessRampIncreaseMaxMillis/DecreaseMaxMillis 3000: 0.073 -> 0.5 would take ~6.7 s.
        assertEquals(3_000L, DozeRamp.durationMs(0.073f, 0.5f, 1f));
        assertEquals(3_000L, DozeRamp.durationMs(0.5f, 0.073f, 1f));
    }

    @Test public void animationScaleStretchesTheRampOrTurnsItOff() {
        long normal = DozeRamp.durationMs(0.30f, 0.35f, 1f);
        assertEquals(2 * normal, DozeRamp.durationMs(0.30f, 0.35f, 2f), 3L);
        assertEquals(6_000L, DozeRamp.durationMs(0.073f, 0.5f, 2f));
        assertEquals(0L, DozeRamp.durationMs(0.073f, 0.5f, 0f));
    }

    @Test public void noChangeMeansNoRamp() {
        assertEquals(0L, DozeRamp.durationMs(0.3f, 0.3f, 1f));
    }

    @Test public void unknownStartMeansUnknownRamp() {
        assertEquals(DozeRamp.UNKNOWN, DozeRamp.durationMs(Float.NaN, 0.5f, 1f));
        assertEquals(DozeRamp.UNKNOWN, DozeRamp.durationMs(0.3f, Float.NaN, 1f));
    }

    @Test public void extraBrightFollowsRightAfterTheRamp() {
        assertEquals("the proven v1.5.x delay when nothing ramps", 250L, DozeRamp.hbmSettleMs(0L));
        assertEquals(646L, DozeRamp.hbmSettleMs(496L));
        assertEquals(3_150L, DozeRamp.hbmSettleMs(3_000L));
        assertEquals("an unknown ramp may be the longest one", 3_150L, DozeRamp.hbmSettleMs(DozeRamp.UNKNOWN));
        assertEquals("a huge animation scale does not hold Extra Bright back for long",
                DozeRamp.MAX_HBM_SETTLE_MS, DozeRamp.hbmSettleMs(60_000L));
    }

    @Test public void aChangeTheSystemAppliedAtOnceNeedsNoWaitForTheRamp() {
        assertEquals(250L, DozeRamp.settleMs(3_000L, true));
        assertEquals(250L, DozeRamp.settleMs(DozeRamp.UNKNOWN, true));
        assertEquals(3_150L, DozeRamp.settleMs(3_000L, false));
        assertEquals(646L, DozeRamp.settleMs(496L, false));
    }
}
