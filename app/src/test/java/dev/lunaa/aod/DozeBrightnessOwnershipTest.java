package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DozeBrightnessOwnershipTest {
    private static final int DISPLAY_OFF = 1;
    private static final int DISPLAY_ON = 2;
    private static final int DISPLAY_DOZE = 3;
    private static final int DISPLAY_DOZE_SUSPEND = 4;

    @Test public void freshAodSessionWaitsUntilTheScreenLeavesFullPower() {
        DozeBrightnessOwnership ownership = new DozeBrightnessOwnership();

        assertFalse(ownership.onDozeState("INITIALIZED"));
        assertFalse(ownership.onDozeState("DOZE_AOD"));

        assertFalse("screen-off animation still runs at full power",
                ownership.reclaim(DISPLAY_ON));
        assertFalse(ownership.isOwnedByModule());

        assertTrue(ownership.reclaim(DISPLAY_DOZE));
        assertTrue(ownership.isOwnedByModule());
    }

    @Test public void freshAodSessionPlaysTheScreenOffAnimationUntilTheModuleTakesOver() {
        DozeBrightnessOwnership ownership = new DozeBrightnessOwnership();
        ownership.onDozeState("INITIALIZED");
        assertFalse("not in AOD yet", ownership.isScreenOffAnimation());

        ownership.onDozeState("DOZE_AOD");
        ownership.reclaim(DISPLAY_ON);
        assertTrue(ownership.isScreenOffAnimation());

        ownership.reclaim(DISPLAY_DOZE_SUSPEND);
        assertFalse(ownership.isScreenOffAnimation());
    }

    @Test public void pulseScreenIsNotTheScreenOffAnimation() {
        DozeBrightnessOwnership ownership = new DozeBrightnessOwnership();
        ownership.onDozeState("DOZE_AOD");
        ownership.onDozeState("DOZE_REQUEST_PULSE");
        assertFalse(ownership.isScreenOffAnimation());

        ownership.onDozeState("DOZE_PULSE_DONE");
        ownership.onDozeState("DOZE_AOD");
        assertFalse("back in AOD, the display still shows the pulse at full power",
                ownership.isScreenOffAnimation());
    }

    @Test public void screenOffAnimationKeepsTheBrightnessTheScreenShows() {
        assertEquals(0.35f, DozeBrightnessOwnership.screenOffAnimationBrightness(0.35f, 0.35f), 0.0001f);
        assertEquals("dimmed before a timeout: do not brighten back to the setting",
                0.035f, DozeBrightnessOwnership.screenOffAnimationBrightness(0.035f, 0.35f), 0.0001f);
    }

    @Test public void screenOffAnimationIsNeverBrighterThanTheBrightnessSetting() {
        assertEquals("HDR video or an app brightness override does not carry over to the lock screen",
                0.35f, DozeBrightnessOwnership.screenOffAnimationBrightness(1.0f, 0.35f), 0.0001f);
    }

    @Test public void screenOffAnimationStaysWithinTheModuleBrightnessRange() {
        assertEquals(1.0f, DozeBrightnessOwnership.screenOffAnimationBrightness(1.5f, 1.5f), 0.0001f);
        assertEquals(0.010f, DozeBrightnessOwnership.screenOffAnimationBrightness(0.0f, 0.35f), 0.0001f);
    }

    @Test public void screenOffAnimationUsesWhicheverBrightnessIsKnown() {
        assertEquals(0.35f, DozeBrightnessOwnership.screenOffAnimationBrightness(Float.NaN, 0.35f), 0.0001f);
        assertEquals(0.35f, DozeBrightnessOwnership.screenOffAnimationBrightness(-1.0f, 0.35f), 0.0001f);
        assertEquals(0.20f, DozeBrightnessOwnership.screenOffAnimationBrightness(0.20f, Float.NaN), 0.0001f);
    }

    @Test public void screenOffAnimationLeavesStockAloneWhenNoBrightnessIsUsable() {
        assertTrue(Float.isNaN(DozeBrightnessOwnership.screenOffAnimationBrightness(Float.NaN, Float.NaN)));
        assertTrue(Float.isNaN(DozeBrightnessOwnership.screenOffAnimationBrightness(-1.0f, -1.0f)));
        assertTrue(Float.isNaN(DozeBrightnessOwnership.screenOffAnimationBrightness(
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)));
    }

    @Test public void pulseBeforeTheModuleEverOwnedTheBrightnessHasNothingToHandBack() {
        DozeBrightnessOwnership ownership = new DozeBrightnessOwnership();
        ownership.onDozeState("INITIALIZED");
        ownership.onDozeState("DOZE_AOD");

        assertFalse(ownership.onDozeState("DOZE_REQUEST_PULSE"));
        assertFalse(ownership.isOwnedByModule());
    }

    @Test public void udfpsPulseHandsTheBrightnessBackExactlyOnce() {
        DozeBrightnessOwnership ownership = aodOwnedByModule();

        assertTrue(ownership.onDozeState("DOZE_REQUEST_PULSE"));
        assertFalse(ownership.onDozeState("DOZE_PULSING"));
        assertFalse(ownership.onDozeState("FINISH"));

        assertFalse(ownership.isOwnedByModule());
    }

    @Test public void wakeUpWithoutPulseAlsoHandsTheBrightnessBack() {
        DozeBrightnessOwnership ownership = aodOwnedByModule();

        assertTrue(ownership.onDozeState("FINISH"));
        assertFalse(ownership.isOwnedByModule());
    }

    @Test public void stockKeepsTheBrightnessWhilePulsingOrWhileTheScreenIsStillOn() {
        DozeBrightnessOwnership ownership = aodOwnedByModule();
        ownership.onDozeState("DOZE_REQUEST_PULSE");

        assertFalse("still pulsing, even if the panel has not switched yet",
                ownership.reclaim(DISPLAY_DOZE_SUSPEND));

        ownership.onDozeState("DOZE_PULSE_DONE");
        ownership.onDozeState("DOZE_AOD");
        assertFalse("back in AOD but the pulse screen is still at full power",
                ownership.reclaim(DISPLAY_ON));
        assertFalse(ownership.isOwnedByModule());
    }

    @Test public void moduleReclaimsOnceAodIsBackAndTheScreenLeftFullPower() {
        DozeBrightnessOwnership ownership = aodOwnedByModule();
        ownership.onDozeState("DOZE_REQUEST_PULSE");
        ownership.onDozeState("DOZE_PULSE_DONE");
        ownership.onDozeState("DOZE_AOD");

        assertTrue(ownership.reclaim(DISPLAY_DOZE_SUSPEND));
        assertTrue(ownership.isOwnedByModule());
        assertTrue("the next pulse hands it back again", ownership.onDozeState("DOZE_REQUEST_PULSE"));
    }

    @Test public void handbackIsNeverBrighterThanTheNormalScreenOrTheAod() {
        assertEquals(0.35f, DozeBrightnessOwnership.handbackBrightness(1.0f, 0.35f), 0.0001f);
        assertEquals(0.20f, DozeBrightnessOwnership.handbackBrightness(0.20f, 0.50f), 0.0001f);
    }

    @Test public void handbackFallsBackToTheDimmestSafeLevelWhenScreenBrightnessIsUnusable() {
        assertEquals(0.010f, DozeBrightnessOwnership.handbackBrightness(1.0f, Float.NaN), 0.0001f);
        assertEquals(0.010f, DozeBrightnessOwnership.handbackBrightness(1.0f, -1.0f), 0.0001f);
        assertEquals(0.010f, DozeBrightnessOwnership.handbackBrightness(1.0f, 0.0f), 0.0001f);
    }

    @Test public void nothingToHandBackWhenTheModuleNeverWroteABrightness() {
        assertTrue(Float.isNaN(DozeBrightnessOwnership.handbackBrightness(Float.NaN, 0.30f)));
    }

    @Test public void moduleMayReclaimWhileTheScreenIsOff() {
        DozeBrightnessOwnership ownership = aodOwnedByModule();
        ownership.onDozeState("DOZE_REQUEST_PULSE");
        ownership.onDozeState("DOZE_AOD_PAUSED");

        assertTrue(ownership.reclaim(DISPLAY_OFF));
    }

    private static DozeBrightnessOwnership aodOwnedByModule() {
        DozeBrightnessOwnership ownership = new DozeBrightnessOwnership();
        ownership.onDozeState("DOZE_AOD");
        assertTrue(ownership.reclaim(DISPLAY_DOZE_SUSPEND));
        return ownership;
    }
}
