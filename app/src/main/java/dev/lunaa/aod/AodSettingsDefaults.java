package dev.lunaa.aod;

public final class AodSettingsDefaults {
    public static final float DEFAULT_MINIMUM_AUTO_BRIGHTNESS = 0.010f;
    public static final int DEFAULT_MANUAL_LEVEL = 2;
    public static final int DEFAULT_MANUAL_LEVEL_1_PERCENT = 10;
    public static final int DEFAULT_MANUAL_LEVEL_2_PERCENT = 50;
    public static final int DEFAULT_MANUAL_LEVEL_3_PERCENT = 100;
    public static final int DEFAULT_EXTRA_BRIGHT_LEVEL = 1;
    public static final int DEFAULT_EXTRA_LEVEL_1_PERCENT = 50;
    public static final int DEFAULT_EXTRA_LEVEL_2_PERCENT = 75;
    public static final int DEFAULT_EXTRA_LEVEL_3_PERCENT = 100;
    public static final float DEFAULT_MANUAL_BRIGHTNESS = DEFAULT_MANUAL_LEVEL_2_PERCENT / 100f;

    private static final float[] LUX = {
            0f, 2f, 10f, 50f, 200f, 500f, 1000f, 5000f, 20000f
    };

    private static final float[] DIM = {
            0.200f, 0.200f, 0.220f, 0.250f, 0.280f, 0.300f, 0.320f, 0.350f, 0.400f
    };

    private static final float[] BALANCED = {
            0.500f, 0.500f, 0.540f, 0.600f, 0.690f, 0.760f, 0.830f, 0.930f, 1.000f
    };

    private static final float[] BRIGHT = {
            1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f, 1.000f
    };

    private static final String[] HINTS = {
            "almost complete darkness",
            "very dark room",
            "dim room",
            "evening / low indoor light",
            "typical indoor light",
            "bright indoor light",
            "bright window / shaded daylight",
            "strong daylight",
            "very bright daylight"
    };

    private AodSettingsDefaults() {}

    public static AodSettingsSnapshot dim() { return create(AodPreset.DIM, DIM); }
    public static AodSettingsSnapshot balanced() { return create(AodPreset.BALANCED, BALANCED); }
    public static AodSettingsSnapshot bright() { return create(AodPreset.BRIGHT, BRIGHT); }
    public static AodSettingsSnapshot disabledBalanced() { return create(false, AodPreset.BALANCED, BALANCED); }
    public static String hintAt(int index) { return HINTS[index]; }

    public static AodSettingsSnapshot forPreset(AodPreset preset) {
        if (preset == AodPreset.DIM) return dim();
        if (preset == AodPreset.BRIGHT) return bright();
        return balanced();
    }

    private static AodSettingsSnapshot create(AodPreset preset, float[] brightness) {
        return create(true, preset, brightness);
    }

    private static AodSettingsSnapshot create(boolean enabled, AodPreset preset, float[] brightness) {
        return new AodSettingsSnapshot(
                enabled,
                AodMode.AUTOMATIC,
                100,
                DEFAULT_MINIMUM_AUTO_BRIGHTNESS,
                preset,
                AodPreset.DIM.defaultCapPercent(),
                AodPreset.BALANCED.defaultCapPercent(),
                AodPreset.BRIGHT.defaultCapPercent(),
                DEFAULT_MANUAL_LEVEL,
                DEFAULT_MANUAL_LEVEL_1_PERCENT,
                DEFAULT_MANUAL_LEVEL_2_PERCENT,
                DEFAULT_MANUAL_LEVEL_3_PERCENT,
                DEFAULT_EXTRA_BRIGHT_LEVEL,
                DEFAULT_EXTRA_LEVEL_1_PERCENT,
                DEFAULT_EXTRA_LEVEL_2_PERCENT,
                DEFAULT_EXTRA_LEVEL_3_PERCENT,
                LUX,
                brightness,
                0
        );
    }
}
