package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class AodPresetTest {
    @Test public void detectsThreeBuiltInCurves() {
        assertEquals(AodPreset.DIM, AodPreset.detect(AodSettingsDefaults.dim()));
        assertEquals(AodPreset.BALANCED, AodPreset.detect(AodSettingsDefaults.balanced()));
        assertEquals(AodPreset.BRIGHT, AodPreset.detect(AodSettingsDefaults.bright()));
    }

    @Test public void presetsExposeV140CapRangesAndDefaults() {
        assertEquals(0, AodPreset.DIM.minCapPercent());
        assertEquals(40, AodPreset.DIM.maxCapPercent());
        assertEquals(25, AodPreset.DIM.defaultCapPercent());
        assertEquals(40, AodPreset.BALANCED.minCapPercent());
        assertEquals(100, AodPreset.BALANCED.maxCapPercent());
        assertEquals(100, AodPreset.BALANCED.defaultCapPercent());
        assertEquals(70, AodPreset.BRIGHT.minCapPercent());
        assertEquals(100, AodPreset.BRIGHT.maxCapPercent());
        assertEquals(100, AodPreset.BRIGHT.defaultCapPercent());
    }

    @Test public void builtInCurvesUseV140Calibration() {
        assertArrayEquals(
                new float[]{0.200f, 0.200f, 0.220f, 0.250f, 0.280f, 0.300f, 0.320f, 0.350f, 0.400f},
                AodSettingsDefaults.dim().copyBrightness(),
                0.000001f
        );
        assertArrayEquals(
                new float[]{0.500f, 0.500f, 0.540f, 0.600f, 0.690f, 0.760f, 0.830f, 0.930f, 1.000f},
                AodSettingsDefaults.balanced().copyBrightness(),
                0.000001f
        );
        assertArrayEquals(
                new float[]{1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f},
                AodSettingsDefaults.bright().copyBrightness(),
                0.000001f
        );
    }

    @Test public void ignoresNonCurveSettingsWhenDetectingPreset() {
        AodSettingsSnapshot base = AodSettingsDefaults.bright();
        AodSettingsSnapshot changedControls = new AodSettingsSnapshot(
                false,
                AodMode.MANUAL,
                275,
                0.180f,
                0.444f,
                base.copyLux(),
                base.copyBrightness(),
                99
        );
        assertEquals(AodPreset.BRIGHT, AodPreset.detect(changedControls));
    }

    @Test public void editedCurveBecomesCustom() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        float[] brightness = base.copyBrightness();
        brightness[4] += 0.010f;
        AodSettingsSnapshot edited = new AodSettingsSnapshot(
                true,
                AodMode.AUTOMATIC,
                100,
                0.020f,
                0.150f,
                base.copyLux(),
                brightness,
                0
        );
        assertEquals(AodPreset.CUSTOM, AodPreset.detect(edited));
    }
}
