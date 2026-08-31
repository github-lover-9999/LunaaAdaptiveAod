package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BrightnessCurveTest {
    @Test public void balancedUsesFormerBrightNormalEnvelopeAndDimStaysLower() {
        AodSettingsSnapshot dim = AodSettingsDefaults.dim();
        AodSettingsSnapshot balanced = AodSettingsDefaults.balanced();
        AodSettingsSnapshot brightDaylight = AodSettingsDefaults.bright();
        for (float lux : new float[]{50f, 500f, 1000f, 5000f}) {
            float d = BrightnessCurve.targetForLux(lux, dim);
            float b = BrightnessCurve.targetForLux(lux, balanced);
            float h = BrightnessCurve.targetForLux(lux, brightDaylight);
            assertTrue("DIM must be visibly below BALANCED at " + lux, b - d >= 0.08f);
            assertEquals("Bright Daylight is full normal brightness (100%)", 1.000f, h, 0.0001f);
        }
    }

    @Test public void balancedDefaultEnvelopeHasExpectedReferencePoints() {
        AodSettingsSnapshot s = AodSettingsDefaults.balanced();
        assertEquals(0.500f, BrightnessCurve.targetForLux(0f, s), 0.0001f);
        assertEquals(0.600f, BrightnessCurve.targetForLux(50f, s), 0.001f);
        assertEquals(0.760f, BrightnessCurve.targetForLux(500f, s), 0.001f);
        assertEquals(1.000f, BrightnessCurve.targetForLux(20000f, s), 0.0001f);
    }

    @Test public void selectedPresetCapScalesWholeAutomaticEnvelope() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        AodSettingsSnapshot capped = configured(base, AodPreset.BALANCED, 25, 45, 100, 250, 0.20f);
        float defaultAt500 = BrightnessCurve.targetForLux(500f, base);
        float cappedAt500 = BrightnessCurve.targetForLux(500f, capped);
        assertTrue(defaultAt500 - cappedAt500 >= 0.045f);
        assertEquals(0.450f, BrightnessCurve.targetForLux(20000f, capped), 0.0001f);
    }

    @Test public void legacyMultiplierAndMinimumNoLongerChangeAutomaticOutput() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        AodSettingsSnapshot legacyControls = configured(
                base, AodPreset.BALANCED, 25, 100, 100, 300, 0.50f);
        assertEquals(0.500f, BrightnessCurve.targetForLux(0f, legacyControls), 0.0001f);
        assertEquals(0.600f, BrightnessCurve.targetForLux(50f, legacyControls), 0.001f);
        assertEquals(1.000f, BrightnessCurve.targetForLux(20000f, legacyControls), 0.0001f);
    }

    @Test public void dimCurveRespectsItsFortyPercentCeiling() {
        AodSettingsSnapshot dim = AodSettingsDefaults.dim();
        assertEquals(0.243f, BrightnessCurve.targetForLux(5000f, dim), 0.001f);
        assertEquals(0.250f, BrightnessCurve.targetForLux(20000f, dim), 0.0001f);
    }

    @Test public void zeroDimCapMapsToInternalOnePercentFloor() {
        AodSettingsSnapshot base = AodSettingsDefaults.dim();
        AodSettingsSnapshot zeroCap = configured(base, AodPreset.DIM, 0, 80, 100, 100, 0.01f);
        assertEquals(0.010f, BrightnessCurve.targetForLux(20000f, zeroCap), 0.0001f);
    }

    @Test public void invalidLuxUsesSettingsAwareFallback() {
        AodSettingsSnapshot s = AodSettingsDefaults.balanced();
        assertEquals(0.500f, BrightnessCurve.targetForLux(Float.NaN, s), 0.0001f);
        assertEquals(0.500f, BrightnessCurve.targetForLux(-1f, s), 0.0001f);
    }

    @Test public void screenBrightnessSeedIsLimitedByPresetCapOnly() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        assertEquals(0.5150f, BrightnessCurve.initialFromScreenBrightness(0.05f, base), 0.0002f);
        assertEquals(0.5441f, BrightnessCurve.initialFromScreenBrightness(0.147f, base), 0.0003f);

        AodSettingsSnapshot legacyControls = configured(
                base, AodPreset.BALANCED, 25, 100, 100, 300, 0.50f);
        assertEquals(0.5150f, BrightnessCurve.initialFromScreenBrightness(0.05f, legacyControls), 0.0002f);

        AodSettingsSnapshot dimCap = configured(
                AodSettingsDefaults.dim(), AodPreset.DIM, 20, 80, 100, 100, 0.01f);
        assertEquals(0.2000f, BrightnessCurve.initialFromScreenBrightness(1.0f, dimCap), 0.0002f);
    }

    private static AodSettingsSnapshot configured(
            AodSettingsSnapshot base,
            AodPreset preset,
            int dimCap,
            int balancedCap,
            int brightCap,
            int multiplier,
            float minimum
    ) {
        return new AodSettingsSnapshot(
                true,
                AodMode.AUTOMATIC,
                multiplier,
                minimum,
                0.150f,
                preset,
                dimCap,
                balancedCap,
                brightCap,
                base.copyLux(),
                base.copyBrightness(),
                1
        );
    }
}
