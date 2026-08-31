package dev.lunaa.aod;

public final class AodSettingsCodec {
    public static final String PREF_FILE = "aod_settings";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_MODE = "mode";
    public static final String KEY_MULTIPLIER_PERCENT = "multiplier_percent";
    public static final String KEY_MINIMUM_AUTO_BRIGHTNESS = "minimum_auto_brightness";
    public static final String KEY_MANUAL_BRIGHTNESS = "manual_brightness";
    public static final String KEY_PRESET = "preset";
    public static final String KEY_DIM_CAP_PERCENT = "dim_cap_percent";
    public static final String KEY_BALANCED_CAP_PERCENT = "balanced_cap_percent";
    public static final String KEY_BRIGHT_CAP_PERCENT = "bright_cap_percent";
    public static final String KEY_EXTRA_BRIGHT_PERCENT = "extra_bright_percent";
    public static final String KEY_MANUAL_EXTRA_BRIGHT_ENABLED = "manual_extra_bright_enabled";
    public static final String KEY_AUTOMATIC_EXTRA_BRIGHT_ENABLED = "automatic_extra_bright_enabled";
    public static final String KEY_MANUAL_LEVEL = "manual_level";
    public static final String KEY_EXTRA_BRIGHT_LEVEL = "extra_bright_level";
    public static final String KEY_MANUAL_LEVEL_1_PERCENT = "manual_level_1_percent";
    public static final String KEY_MANUAL_LEVEL_2_PERCENT = "manual_level_2_percent";
    public static final String KEY_MANUAL_LEVEL_3_PERCENT = "manual_level_3_percent";
    public static final String KEY_EXTRA_LEVEL_1_PERCENT = "extra_level_1_percent";
    public static final String KEY_EXTRA_LEVEL_2_PERCENT = "extra_level_2_percent";
    public static final String KEY_EXTRA_LEVEL_3_PERCENT = "extra_level_3_percent";
    public static final String KEY_REVISION = "revision";
    public static final String KEY_AUTO_PROFILE_VERSION = "auto_profile_version";
    public static final int CURRENT_AUTO_PROFILE_VERSION = 3;

    private static final int MISSING_INT = Integer.MIN_VALUE;
    private static final float TOLERANCE = 0.0005f;
    private static final float[] LEGACY_DIM = {
            0.025f, 0.030f, 0.045f, 0.080f, 0.130f, 0.190f, 0.260f, 0.400f, 0.520f
    };
    private static final float[] LEGACY_BALANCED = {
            0.035f, 0.045f, 0.070f, 0.130f, 0.210f, 0.300f, 0.400f, 0.580f, 0.720f
    };
    private static final float[] LEGACY_BRIGHT = {
            0.050f, 0.065f, 0.100f, 0.190f, 0.300f, 0.420f, 0.540f, 0.720f, 0.850f
    };

    public interface Reader {
        boolean getBoolean(String key, boolean fallback);
        int getInt(String key, int fallback);
        float getFloat(String key, float fallback);
    }

    public interface Writer {
        void putBoolean(String key, boolean value);
        void putInt(String key, int value);
        void putFloat(String key, float value);
        boolean commit();
    }

    private AodSettingsCodec() {}

    public static String luxKey(int index) {
        requirePointIndex(index);
        return "lux_" + index;
    }

    public static String brightnessKey(int index) {
        requirePointIndex(index);
        return "brightness_" + index;
    }

    public static AodSettingsSnapshot readOrDefault(Reader reader) {
        AodSettingsSnapshot fallback = AodSettingsDefaults.balanced();
        AodSettingsSnapshot disabledFallback = AodSettingsDefaults.disabledBalanced();
        if (reader == null) return disabledFallback;
        try {
            float[] persistedLux = new float[AodSettingsSnapshot.POINT_COUNT];
            float[] persistedBrightness = new float[AodSettingsSnapshot.POINT_COUNT];
            for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
                persistedLux[i] = reader.getFloat(luxKey(i), fallback.luxAt(i));
                persistedBrightness[i] = reader.getFloat(brightnessKey(i), fallback.brightnessAt(i));
            }
            validateLegacyCurve(persistedLux, persistedBrightness);

            int persistedPreset = reader.getInt(KEY_PRESET, MISSING_INT);
            AodPreset preset = persistedPreset == MISSING_INT
                    ? detectMigratedPreset(persistedLux, persistedBrightness)
                    : normalizeSelectablePreset(AodPreset.fromPersistedValue(persistedPreset));
            AodSettingsSnapshot presetDefaults = AodSettingsDefaults.forPreset(preset);

            int storedManual1 = reader.getInt(KEY_MANUAL_LEVEL_1_PERCENT, MISSING_INT);
            int storedManual2 = reader.getInt(KEY_MANUAL_LEVEL_2_PERCENT, MISSING_INT);
            int storedManual3 = reader.getInt(KEY_MANUAL_LEVEL_3_PERCENT, MISSING_INT);
            boolean hasManualMappings = storedManual1 != MISSING_INT
                    && storedManual2 != MISSING_INT
                    && storedManual3 != MISSING_INT;
            int manualLevel1 = hasManualMappings
                    ? storedManual1 : AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_1_PERCENT;
            int manualLevel2 = hasManualMappings
                    ? storedManual2 : AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_2_PERCENT;
            int manualLevel3 = hasManualMappings
                    ? storedManual3 : AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_3_PERCENT;

            int storedExtra1 = reader.getInt(KEY_EXTRA_LEVEL_1_PERCENT, MISSING_INT);
            int storedExtra2 = reader.getInt(KEY_EXTRA_LEVEL_2_PERCENT, MISSING_INT);
            int storedExtra3 = reader.getInt(KEY_EXTRA_LEVEL_3_PERCENT, MISSING_INT);
            boolean hasExtraMappings = storedExtra1 != MISSING_INT
                    && storedExtra2 != MISSING_INT
                    && storedExtra3 != MISSING_INT;
            int extraLevel1 = hasExtraMappings
                    ? storedExtra1 : AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_1_PERCENT;
            int extraLevel2 = hasExtraMappings
                    ? storedExtra2 : AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_2_PERCENT;
            int extraLevel3 = hasExtraMappings
                    ? storedExtra3 : AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_3_PERCENT;

            int persistedManualLevel = reader.getInt(KEY_MANUAL_LEVEL, MISSING_INT);
            int legacyManualPercent = Math.max(1, Math.min(100, Math.round(reader.getFloat(
                    KEY_MANUAL_BRIGHTNESS, fallback.getManualBrightness()) * 100f)));
            int manualLevel;
            if (persistedManualLevel != MISSING_INT) {
                manualLevel = persistedManualLevel;
            } else if (!hasManualMappings) {
                if (legacyManualPercent >= 100) {
                    manualLevel = 3;
                } else if (legacyManualPercent <= AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_1_PERCENT) {
                    manualLevel = 1;
                    manualLevel1 = legacyManualPercent;
                } else {
                    manualLevel = 2;
                    manualLevel2 = legacyManualPercent;
                }
            } else {
                manualLevel = BrightnessLevelConfig.closestLevel(
                        legacyManualPercent, manualLevel1, manualLevel2, manualLevel3);
            }

            int persistedExtraLevel = reader.getInt(KEY_EXTRA_BRIGHT_LEVEL, MISSING_INT);
            int legacyExtraPercent = ExtraBrightnessLevel.normalize(reader.getInt(
                    KEY_EXTRA_BRIGHT_PERCENT, ExtraBrightnessLevel.DEFAULT_PERCENT));
            int extraBrightLevel;
            if (persistedExtraLevel != MISSING_INT) {
                extraBrightLevel = persistedExtraLevel;
            } else if (!hasExtraMappings) {
                if (legacyExtraPercent >= 100) {
                    extraBrightLevel = 3;
                } else if (legacyExtraPercent <= AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_1_PERCENT) {
                    extraBrightLevel = 1;
                    extraLevel1 = legacyExtraPercent;
                } else {
                    extraBrightLevel = 2;
                    extraLevel2 = legacyExtraPercent;
                }
            } else {
                extraBrightLevel = BrightnessLevelConfig.closestLevel(
                        legacyExtraPercent, extraLevel1, extraLevel2, extraLevel3);
            }

            // Validate mappings after migration. Invalid persisted data fails closed.
            new BrightnessLevelConfig(manualLevel, manualLevel1, manualLevel2, manualLevel3);
            new BrightnessLevelConfig(extraBrightLevel, extraLevel1, extraLevel2, extraLevel3);

            // v3 uses fixed automatic profiles. Legacy/custom cap sliders are intentionally ignored.
            reader.getInt(KEY_AUTO_PROFILE_VERSION, 1);
            int dimCap = AodPreset.DIM.defaultCapPercent();
            int balancedCap = AodPreset.BALANCED.defaultCapPercent();
            int brightCap = AodPreset.BRIGHT.defaultCapPercent();
            boolean manualExtraBrightEnabled = reader.getBoolean(
                    KEY_MANUAL_EXTRA_BRIGHT_ENABLED, false);

            return new AodSettingsSnapshot(
                    reader.getBoolean(KEY_ENABLED, fallback.isEnabled()),
                    AodMode.fromPersistedValue(
                            reader.getInt(KEY_MODE, fallback.getMode().persistedValue())
                    ),
                    100,
                    AodSettingsSnapshot.MIN_BRIGHTNESS,
                    preset,
                    dimCap,
                    balancedCap,
                    brightCap,
                    manualLevel,
                    manualLevel1,
                    manualLevel2,
                    manualLevel3,
                    manualExtraBrightEnabled,
                    extraBrightLevel,
                    extraLevel1,
                    extraLevel2,
                    extraLevel3,
                    presetDefaults.copyLux(),
                    presetDefaults.copyBrightness(),
                    reader.getInt(KEY_REVISION, fallback.getRevision())
            );
        } catch (RuntimeException ignored) {
            return disabledFallback;
        }
    }

    public static boolean write(Writer writer, AodSettingsSnapshot snapshot) {
        if (writer == null || snapshot == null) return false;
        AodPreset detected = AodPreset.detect(snapshot);
        AodPreset preset = detected == AodPreset.CUSTOM
                ? normalizeSelectablePreset(snapshot.getPreset())
                : normalizeSelectablePreset(detected);
        AodSettingsSnapshot curve = AodSettingsDefaults.forPreset(preset);
        writer.putBoolean(KEY_ENABLED, snapshot.isEnabled());
        writer.putInt(KEY_MODE, snapshot.getMode().persistedValue());
        writer.putInt(KEY_MULTIPLIER_PERCENT, 100);
        writer.putFloat(KEY_MINIMUM_AUTO_BRIGHTNESS, AodSettingsSnapshot.MIN_BRIGHTNESS);
        writer.putFloat(KEY_MANUAL_BRIGHTNESS, snapshot.getManualBrightness());
        writer.putInt(KEY_PRESET, preset.persistedValue());
        writer.putInt(KEY_DIM_CAP_PERCENT, AodPreset.DIM.defaultCapPercent());
        writer.putInt(KEY_BALANCED_CAP_PERCENT, AodPreset.BALANCED.defaultCapPercent());
        writer.putInt(KEY_BRIGHT_CAP_PERCENT, AodPreset.BRIGHT.defaultCapPercent());
        writer.putInt(KEY_EXTRA_BRIGHT_PERCENT, snapshot.getExtraBrightPercent());
        writer.putBoolean(KEY_MANUAL_EXTRA_BRIGHT_ENABLED, snapshot.isManualExtraBrightEnabled());
        writer.putInt(KEY_MANUAL_LEVEL, snapshot.getManualLevel());
        writer.putInt(KEY_EXTRA_BRIGHT_LEVEL, snapshot.getExtraBrightLevel());
        writer.putInt(KEY_MANUAL_LEVEL_1_PERCENT, snapshot.getManualLevelPercent(1));
        writer.putInt(KEY_MANUAL_LEVEL_2_PERCENT, snapshot.getManualLevelPercent(2));
        writer.putInt(KEY_MANUAL_LEVEL_3_PERCENT, snapshot.getManualLevelPercent(3));
        writer.putInt(KEY_EXTRA_LEVEL_1_PERCENT, snapshot.getExtraBrightLevelPercent(1));
        writer.putInt(KEY_EXTRA_LEVEL_2_PERCENT, snapshot.getExtraBrightLevelPercent(2));
        writer.putInt(KEY_EXTRA_LEVEL_3_PERCENT, snapshot.getExtraBrightLevelPercent(3));
        writer.putInt(KEY_REVISION, snapshot.getRevision());
        writer.putInt(KEY_AUTO_PROFILE_VERSION, CURRENT_AUTO_PROFILE_VERSION);
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            writer.putFloat(luxKey(i), curve.luxAt(i));
            writer.putFloat(brightnessKey(i), curve.brightnessAt(i));
        }
        return writer.commit();
    }

    private static AodPreset detectMigratedPreset(float[] lux, float[] brightness) {
        if (matchesLegacyBrightness(brightness, LEGACY_DIM)) return AodPreset.DIM;
        if (matchesLegacyBrightness(brightness, LEGACY_BRIGHT)) return AodPreset.BRIGHT;
        if (matchesLegacyBrightness(brightness, LEGACY_BALANCED)) return AodPreset.BALANCED;
        AodSettingsSnapshot candidate = new AodSettingsSnapshot(
                true,
                AodMode.AUTOMATIC,
                100,
                AodSettingsSnapshot.MIN_BRIGHTNESS,
                AodSettingsDefaults.DEFAULT_MANUAL_BRIGHTNESS,
                lux,
                brightness,
                0
        );
        AodPreset detected = AodPreset.detect(candidate);
        return normalizeSelectablePreset(detected);
    }

    private static boolean matchesLegacyBrightness(float[] actual, float[] expected) {
        for (int i = 0; i < expected.length; i++) {
            if (Math.abs(actual[i] - expected[i]) > TOLERANCE) return false;
        }
        return true;
    }

    private static AodPreset normalizeSelectablePreset(AodPreset preset) {
        return preset == AodPreset.DIM || preset == AodPreset.BRIGHT
                ? preset
                : AodPreset.BALANCED;
    }

    private static void validateLegacyCurve(float[] lux, float[] brightness) {
        new AodSettingsSnapshot(
                true,
                AodMode.AUTOMATIC,
                100,
                AodSettingsSnapshot.MIN_BRIGHTNESS,
                AodSettingsDefaults.DEFAULT_MANUAL_BRIGHTNESS,
                lux,
                brightness,
                0
        );
    }

    private static void requirePointIndex(int index) {
        if (index < 0 || index >= AodSettingsSnapshot.POINT_COUNT) {
            throw new IllegalArgumentException("curve point index out of range: " + index);
        }
    }
}
