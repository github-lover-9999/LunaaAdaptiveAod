package dev.lunaa.aod;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class AodSettingsTest {
    @Test public void balancedPresetUsesFixedFullRangeAndFiftyPercentManualDefault() {
        AodSettingsSnapshot s = AodSettingsDefaults.balanced();
        assertTrue(s.isEnabled());
        assertEquals(AodMode.AUTOMATIC, s.getMode());
        assertEquals(100, s.getMultiplierPercent());
        assertEquals(0.010f, s.getMinimumAutoBrightness(), 0.0001f);
        assertEquals(0.500f, s.getManualBrightness(), 0.0001f);
        assertEquals(AodPreset.BALANCED, s.getPreset());
        assertEquals(25, s.getDimCapPercent());
        assertEquals(100, s.getBalancedCapPercent());
        assertEquals(100, s.getBrightCapPercent());
        assertEquals(50, s.getExtraBrightPercent());
        assertEquals(100, s.getAutomaticCapPercent());
        assertFalse(s.isManualExtraBrightEnabled());
        assertEquals(1, s.getExtraBrightLevel());
        assertEquals(0, s.getRevision());
        assertArrayEquals(new float[]{0f, 2f, 10f, 50f, 200f, 500f, 1000f, 5000f, 20000f}, s.copyLux(), 0.0001f);
        assertArrayEquals(new float[]{0.500f, 0.500f, 0.540f, 0.600f, 0.690f, 0.760f, 0.830f, 0.930f, 1.000f}, s.copyBrightness(), 0.0001f);
    }


    @Test public void codecPersistsFixedAutomaticProfilesAndManualExtraBrightState() {
        AodSettingsSnapshot base = AodSettingsDefaults.bright();
        AodSettingsSnapshot original = new AodSettingsSnapshot(
                true, AodMode.AUTOMATIC, 100, 0.010f,
                AodPreset.BRIGHT, 22, 73, 95,
                3, 10, 50, 100,
                true,
                2, 50, 88, 100,
                base.copyLux(), base.copyBrightness(), 17);
        MapWriter writer = new MapWriter();
        assertTrue(AodSettingsCodec.write(writer, original));
        assertEquals(AodPreset.BRIGHT.persistedValue(), writer.values.get(AodSettingsCodec.KEY_PRESET));
        assertEquals(25, writer.values.get(AodSettingsCodec.KEY_DIM_CAP_PERCENT));
        assertEquals(100, writer.values.get(AodSettingsCodec.KEY_BALANCED_CAP_PERCENT));
        assertEquals(100, writer.values.get(AodSettingsCodec.KEY_BRIGHT_CAP_PERCENT));
        assertEquals(true, writer.values.get(AodSettingsCodec.KEY_MANUAL_EXTRA_BRIGHT_ENABLED));

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(writer.values));
        assertEquals(AodPreset.BRIGHT, loaded.getPreset());
        assertEquals(25, loaded.getDimCapPercent());
        assertEquals(100, loaded.getBalancedCapPercent());
        assertEquals(100, loaded.getBrightCapPercent());
        assertEquals(100, loaded.getAutomaticCapPercent());
        assertTrue(loaded.isManualExtraBrightEnabled());
        assertEquals(2, loaded.getExtraBrightLevel());
        assertEquals(88, loaded.getExtraBrightPercent());
    }

    @Test public void legacyV131PreferencesMigratePresetAndCapsWithoutKeepingOldMultiplier() {
        float[] legacyLux = {0f, 2f, 10f, 50f, 200f, 500f, 1000f, 5000f, 20000f};
        float[] legacyBright = {0.050f, 0.065f, 0.100f, 0.190f, 0.300f, 0.420f, 0.540f, 0.720f, 0.850f};
        Map<String, Object> values = new HashMap<>();
        values.put(AodSettingsCodec.KEY_ENABLED, true);
        values.put(AodSettingsCodec.KEY_MODE, AodMode.AUTOMATIC.persistedValue());
        values.put(AodSettingsCodec.KEY_MULTIPLIER_PERCENT, 300);
        values.put(AodSettingsCodec.KEY_MANUAL_BRIGHTNESS, 0.77f);
        values.put(AodSettingsCodec.KEY_REVISION, 12);
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            values.put(AodSettingsCodec.luxKey(i), legacyLux[i]);
            values.put(AodSettingsCodec.brightnessKey(i), legacyBright[i]);
        }

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(values));
        assertEquals(AodPreset.BRIGHT, loaded.getPreset());
        assertEquals(100, loaded.getMultiplierPercent());
        assertEquals(25, loaded.getDimCapPercent());
        assertEquals(100, loaded.getBalancedCapPercent());
        assertEquals(100, loaded.getBrightCapPercent());
        assertEquals(0.77f, loaded.getManualBrightness(), 0.0001f);
    }

    @Test public void legacyDefaultCapsMigrateToFixedV3Profiles() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        Map<String, Object> values = new HashMap<>();
        values.put(AodSettingsCodec.KEY_PRESET, AodPreset.BALANCED.persistedValue());
        values.put(AodSettingsCodec.KEY_DIM_CAP_PERCENT, 35);
        values.put(AodSettingsCodec.KEY_BALANCED_CAP_PERCENT, 80);
        values.put(AodSettingsCodec.KEY_BRIGHT_CAP_PERCENT, 100);
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            values.put(AodSettingsCodec.luxKey(i), base.luxAt(i));
            values.put(AodSettingsCodec.brightnessKey(i), base.brightnessAt(i));
        }

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(values));
        assertEquals(25, loaded.getDimCapPercent());
        assertEquals(100, loaded.getBalancedCapPercent());
        assertEquals(100, loaded.getBrightCapPercent());
    }

    @Test public void legacyCustomCapsAreIgnoredByFixedV3Profiles() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        Map<String, Object> values = new HashMap<>();
        values.put(AodSettingsCodec.KEY_PRESET, AodPreset.BALANCED.persistedValue());
        values.put(AodSettingsCodec.KEY_DIM_CAP_PERCENT, 35);
        values.put(AodSettingsCodec.KEY_BALANCED_CAP_PERCENT, 60);
        values.put(AodSettingsCodec.KEY_BRIGHT_CAP_PERCENT, 100);
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            values.put(AodSettingsCodec.luxKey(i), base.luxAt(i));
            values.put(AodSettingsCodec.brightnessKey(i), base.brightnessAt(i));
        }

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(values));
        assertEquals(25, loaded.getDimCapPercent());
        assertEquals(100, loaded.getBalancedCapPercent());
        assertEquals(100, loaded.getBrightCapPercent());
    }

    @Test public void rejectsPresetCapsOutsideTheirUiRanges() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        expectIllegalArgument(() -> new AodSettingsSnapshot(
                true, AodMode.AUTOMATIC, 100, 0.010f, 0.15f,
                AodPreset.BALANCED, -1, 80, 100,
                base.copyLux(), base.copyBrightness(), 0));
        expectIllegalArgument(() -> new AodSettingsSnapshot(
                true, AodMode.AUTOMATIC, 100, 0.010f, 0.15f,
                AodPreset.BALANCED, 35, 39, 100,
                base.copyLux(), base.copyBrightness(), 0));
        expectIllegalArgument(() -> new AodSettingsSnapshot(
                true, AodMode.AUTOMATIC, 100, 0.010f, 0.15f,
                AodPreset.BRIGHT, 35, 80, 101,
                base.copyLux(), base.copyBrightness(), 0));
    }

    @Test public void rejectsInvalidCurveMultiplierAndModeValues() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        float[] lux = base.copyLux();
        float[] brightness = base.copyBrightness();

        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 49, 0.020f, 0.150f, lux, brightness, 0));
        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 301, 0.020f, 0.150f, lux, brightness, 0));
        expectIllegalArgument(() -> snapshot(true, null, 100, 0.020f, 0.150f, lux, brightness, 0));
        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 100, 0.009f, 0.150f, lux, brightness, 0));
        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 100, 1.001f, 0.150f, lux, brightness, 0));
        expectIllegalArgument(() -> snapshot(true, AodMode.MANUAL, 100, 0.020f, 0.009f, lux, brightness, 0));
        expectIllegalArgument(() -> snapshot(true, AodMode.MANUAL, 100, 0.020f, 1.001f, lux, brightness, 0));

        float[] unordered = lux.clone();
        unordered[4] = 10f;
        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 100, 0.020f, 0.150f, unordered, brightness, 0));

        float[] tooLow = brightness.clone();
        tooLow[0] = 0.009f;
        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 100, 0.020f, 0.150f, lux, tooLow, 0));

        float[] tooHigh = brightness.clone();
        tooHigh[8] = 1.001f;
        expectIllegalArgument(() -> snapshot(true, AodMode.AUTOMATIC, 100, 0.020f, 0.150f, lux, tooHigh, 0));
    }

    @Test public void v12PersistedSettingsMigrateNewFieldsWithoutLosingOldValues() {
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        Map<String, Object> values = new HashMap<>();
        values.put(AodSettingsCodec.KEY_ENABLED, false);
        values.put(AodSettingsCodec.KEY_MULTIPLIER_PERCENT, 275);
        values.put(AodSettingsCodec.KEY_REVISION, 8);
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            values.put(AodSettingsCodec.luxKey(i), base.luxAt(i));
            values.put(AodSettingsCodec.brightnessKey(i), base.brightnessAt(i));
        }

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(values));
        assertFalse(loaded.isEnabled());
        assertEquals(AodMode.AUTOMATIC, loaded.getMode());
        assertEquals(100, loaded.getMultiplierPercent());
        assertEquals(0.010f, loaded.getMinimumAutoBrightness(), 0.0001f);
        assertEquals(0.500f, loaded.getManualBrightness(), 0.0001f);
        assertEquals(8, loaded.getRevision());
        assertArrayEquals(base.copyLux(), loaded.copyLux(), 0.0001f);
        assertArrayEquals(base.copyBrightness(), loaded.copyBrightness(), 0.0001f);
    }

    @Test public void codecRoundTripsModeFloorAndManualBrightness() {
        AodSettingsSnapshot base = AodSettingsDefaults.bright();
        AodSettingsSnapshot original = snapshot(
                true,
                AodMode.MANUAL,
                185,
                0.075f,
                0.333f,
                base.copyLux(),
                base.copyBrightness(),
                11
        );
        MapWriter writer = new MapWriter();
        assertTrue(AodSettingsCodec.write(writer, original));

        assertEquals(AodMode.MANUAL.persistedValue(), writer.values.get(AodSettingsCodec.KEY_MODE));
        assertEquals(0.010f, ((Number) writer.values.get(AodSettingsCodec.KEY_MINIMUM_AUTO_BRIGHTNESS)).floatValue(), 0.0001f);
        assertEquals(0.333f, ((Number) writer.values.get(AodSettingsCodec.KEY_MANUAL_BRIGHTNESS)).floatValue(), 0.0001f);

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(writer.values));
        assertEquals(AodMode.MANUAL, loaded.getMode());
        assertEquals(100, loaded.getMultiplierPercent());
        assertEquals(0.010f, loaded.getMinimumAutoBrightness(), 0.0001f);
        assertEquals(0.330f, loaded.getManualBrightness(), 0.0001f);
        assertEquals(11, loaded.getRevision());
        assertArrayEquals(original.copyLux(), loaded.copyLux(), 0.0001f);
        assertArrayEquals(original.copyBrightness(), loaded.copyBrightness(), 0.0001f);
    }

    @Test public void malformedPersistedSettingsFallBackAsWholeSnapshot() {
        Map<String, Object> values = new HashMap<>();
        values.put(AodSettingsCodec.KEY_ENABLED, false);
        values.put(AodSettingsCodec.KEY_MODE, AodMode.MANUAL.persistedValue());
        values.put(AodSettingsCodec.KEY_MULTIPLIER_PERCENT, 275);
        values.put(AodSettingsCodec.KEY_MINIMUM_AUTO_BRIGHTNESS, 0.075f);
        values.put(AodSettingsCodec.KEY_MANUAL_BRIGHTNESS, 0.333f);
        values.put(AodSettingsCodec.KEY_REVISION, 8);
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            values.put(AodSettingsCodec.luxKey(i), AodSettingsDefaults.balanced().luxAt(i));
            values.put(AodSettingsCodec.brightnessKey(i), AodSettingsDefaults.balanced().brightnessAt(i));
        }
        values.put(AodSettingsCodec.luxKey(4), 5f); // breaks strict ordering

        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new MapReader(values));
        AodSettingsSnapshot balanced = AodSettingsDefaults.disabledBalanced();
        assertFalse(loaded.isEnabled());
        assertEquals(AodMode.AUTOMATIC, loaded.getMode());
        assertEquals(100, loaded.getMultiplierPercent());
        assertEquals(0.010f, loaded.getMinimumAutoBrightness(), 0.0001f);
        assertEquals(0.500f, loaded.getManualBrightness(), 0.0001f);
        assertEquals(0, loaded.getRevision());
        assertArrayEquals(balanced.copyLux(), loaded.copyLux(), 0.0001f);
        assertArrayEquals(balanced.copyBrightness(), loaded.copyBrightness(), 0.0001f);
    }


    @Test public void settingsReaderFailureFailsClosedToStockBehavior() {
        AodSettingsSnapshot loaded = AodSettingsCodec.readOrDefault(new AodSettingsCodec.Reader() {
            @Override public boolean getBoolean(String key, boolean fallback) { throw new IllegalStateException("broken"); }
            @Override public int getInt(String key, int fallback) { throw new IllegalStateException("broken"); }
            @Override public float getFloat(String key, float fallback) { throw new IllegalStateException("broken"); }
        });
        assertFalse(loaded.isEnabled());
        assertEquals(AodPreset.BALANCED, loaded.getPreset());
    }

    private static AodSettingsSnapshot snapshot(
            boolean enabled,
            AodMode mode,
            int multiplier,
            float minimum,
            float manual,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        return new AodSettingsSnapshot(
                enabled,
                mode,
                multiplier,
                minimum,
                manual,
                lux,
                brightness,
                revision
        );
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    private static final class MapReader implements AodSettingsCodec.Reader {
        private final Map<String, Object> values;
        MapReader(Map<String, Object> values) { this.values = values; }
        @Override public boolean getBoolean(String key, boolean fallback) {
            Object value = values.get(key);
            return value instanceof Boolean ? (Boolean) value : fallback;
        }
        @Override public int getInt(String key, int fallback) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        }
        @Override public float getFloat(String key, float fallback) {
            Object value = values.get(key);
            return value instanceof Number ? ((Number) value).floatValue() : fallback;
        }
    }

    private static final class MapWriter implements AodSettingsCodec.Writer {
        private final Map<String, Object> values = new HashMap<>();
        @Override public void putBoolean(String key, boolean value) { values.put(key, value); }
        @Override public void putInt(String key, int value) { values.put(key, value); }
        @Override public void putFloat(String key, float value) { values.put(key, value); }
        @Override public boolean commit() { return true; }
    }
}
