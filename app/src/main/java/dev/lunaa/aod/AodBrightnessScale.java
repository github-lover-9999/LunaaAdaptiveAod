package dev.lunaa.aod;

/**
 * The brightest normal (non-HBM) brightness, which Manual Bright (100%) and the top of the
 * automatic presets stand for.
 *
 * <p>On lunaa the backlight range includes the HBM range: the normal range ends at 0.735
 * (backlight 2047 of 2784, the HBM transition point). Android clamps the doze brightness there
 * while HBM is not allowed; without HBM data the brightness slider maximum marks the same end.
 * Levels above it all looked the same (Manual Balanced and Bright, automatic Balanced and Bright),
 * so Manual levels and automatic targets are shares of this brightness instead of the whole range.
 * In AOD the panel runs in its low-power mode, so this level looks much dimmer than on the normal
 * screen; only Extra Bright goes beyond it.</p>
 */
final class AodBrightnessScale {
    /** Below this a reported limit is implausible and ignored. */
    private static final float MIN_PLAUSIBLE = 0.1f;

    private AodBrightnessScale() {}

    /**
     * @param hbmTransitionPoint {@code BrightnessInfo.highBrightnessTransitionPoint}; infinite
     *                           when the ROM has no HBM data
     * @param settingMaximum     {@code config_screenBrightnessSettingMaximumFloat}
     * @return the brightest normal brightness, 1 when nothing limits it
     */
    static float fullScale(float hbmTransitionPoint, float settingMaximum) {
        float scale = 1f;
        if (isLimit(hbmTransitionPoint)) scale = Math.min(scale, hbmTransitionPoint);
        if (isLimit(settingMaximum)) scale = Math.min(scale, settingMaximum);
        return scale;
    }

    private static boolean isLimit(float value) {
        return Float.isFinite(value) && value >= MIN_PLAUSIBLE && value < 1f;
    }
}
