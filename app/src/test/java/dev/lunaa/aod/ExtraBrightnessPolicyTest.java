package dev.lunaa.aod;

import static org.junit.Assert.*;

import org.junit.Test;

public class ExtraBrightnessPolicyTest {
    @Test public void v160AutomaticThresholdsAreReachableButStillDaylightOnly() {
        assertEquals(1500f, ExtraBrightnessPolicy.ENABLE_LUX, 0f);
        assertEquals(700f, ExtraBrightnessPolicy.DISABLE_LUX, 0f);
        assertEquals(600L, ExtraBrightnessPolicy.ENABLE_DWELL_MS);
        assertEquals(1500L, ExtraBrightnessPolicy.DISABLE_DWELL_MS);

        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot bright = snapshot(AodPreset.BRIGHT, 25, 55, 100);
        assertFalse(p.update(1_000, 1_500f, bright, true));
        assertFalse(p.update(1_599, 1_800f, bright, true));
        assertTrue(p.update(1_600, 1_800f, bright, true));
    }

    @Test public void automaticExtraBrightRequiresBrightDaylightPresetButIgnoresBrightCap() {
        for (AodPreset preset : new AodPreset[]{AodPreset.DIM, AodPreset.BALANCED}) {
            ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
            AodSettingsSnapshot settings = snapshot(preset, 25, 55, 100);
            assertFalse(p.update(1_000, 2_000f, settings, true));
            assertFalse(p.update(2_000, 2_000f, settings, true));
        }

        ExtraBrightnessPolicy brightPolicy = new ExtraBrightnessPolicy();
        AodSettingsSnapshot bright = snapshot(AodPreset.BRIGHT, 25, 55, 70);
        assertFalse(brightPolicy.update(1_000, 2_000f, bright, true));
        assertTrue(brightPolicy.update(1_600, 2_000f, bright, true));
    }

    @Test public void hysteresisKeepsExtraBrightOnUntilLuxIsBelowSevenHundredForOnePointFiveSeconds() {
        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot bright = snapshot(AodPreset.BRIGHT, 25, 55, 100);
        p.update(0, 2_000f, bright, true);
        assertTrue(p.update(600, 2_000f, bright, true));
        assertTrue(p.update(1_000, 900f, bright, true));
        assertTrue(p.update(1_500, 650f, bright, true));
        assertTrue(p.update(2_999, 400f, bright, true));
        assertFalse(p.update(3_000, 400f, bright, true));
    }

    @Test public void invalidLuxFailsClosedForAutomaticMode() {
        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot bright = snapshot(AodPreset.BRIGHT, 25, 55, 100);
        p.update(0, 2_000f, bright, true);
        assertTrue(p.update(600, 2_000f, bright, true));
        assertFalse(p.update(601, Float.NaN, bright, true));
        assertFalse(p.update(602, -1f, bright, true));
    }

    @Test public void losingEligibilityOrAmbientStateTurnsExtraBrightOffImmediately() {
        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot bright = snapshot(AodPreset.BRIGHT, 25, 55, 100);
        p.update(0, 2_000f, bright, true);
        assertTrue(p.update(600, 2_000f, bright, true));
        assertFalse(p.update(601, 2_000f, bright, false));

        p.update(1_000, 2_000f, bright, true);
        assertTrue(p.update(1_600, 2_000f, bright, true));
        assertFalse(p.update(1_601, 2_000f, snapshot(AodPreset.BALANCED, 25, 55, 100), true));
    }

    @Test public void automaticBalancedNeverRequestsExtraBright() {
        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot balanced = snapshot(AodPreset.BALANCED, 25, 55, 100);
        assertFalse(p.update(1_000, 10_000f, balanced, true));
        assertFalse(p.update(2_000, 10_000f, balanced, true));
    }

    @Test public void manualBrightRequestsExtraBrightOnlyWhenSeparateToggleIsEnabled() {
        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot base = AodSettingsDefaults.bright();
        AodSettingsSnapshot brightLevel = new AodSettingsSnapshot(
                true, AodMode.MANUAL, 100, AodSettingsSnapshot.MIN_BRIGHTNESS,
                AodPreset.BRIGHT, 25, 55, 100,
                3, 10, 30, 99,
                true,
                2, 50, 75, 100,
                base.copyLux(), base.copyBrightness(), 1);
        assertTrue(p.update(1_000, Float.NaN, brightLevel, true));
    }

    @Test public void manualBrightWithoutSeparateToggleNeverRequestsExtraBright() {
        ExtraBrightnessPolicy p = new ExtraBrightnessPolicy();
        AodSettingsSnapshot base = AodSettingsDefaults.bright();
        AodSettingsSnapshot brightLevel = new AodSettingsSnapshot(
                true, AodMode.MANUAL, 100, AodSettingsSnapshot.MIN_BRIGHTNESS,
                AodPreset.BRIGHT, 25, 100, 100,
                3, 10, 50, 100,
                false,
                1, 50, 75, 100,
                base.copyLux(), base.copyBrightness(), 1);
        assertFalse(p.update(1_000, 10_000f, brightLevel, true));
    }

    private static AodSettingsSnapshot snapshot(
            AodPreset preset, int dimCap, int balancedCap, int brightCap) {
        AodSettingsSnapshot base = AodSettingsDefaults.forPreset(preset);
        return new AodSettingsSnapshot(
                true, AodMode.AUTOMATIC, 100, 0.010f, 0.15f,
                preset, dimCap, balancedCap, brightCap,
                base.copyLux(), base.copyBrightness(), 1);
    }
}
