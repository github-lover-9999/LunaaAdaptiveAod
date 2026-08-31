package dev.lunaa.aod;

/** Pure conversion between the user-facing Extra Bright percentage and black dim-layer alpha. */
public final class ExtraBrightnessLevel {
    public static final int MIN_PERCENT = 1;
    public static final int MAX_PERCENT = 100;
    public static final int DEFAULT_PERCENT = AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_2_PERCENT;

    private ExtraBrightnessLevel() {}

    public static int normalize(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    public static float overlayAlphaForPercent(int percent) {
        return (100 - normalize(percent)) / 100f;
    }
}
