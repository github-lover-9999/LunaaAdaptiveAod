package dev.lunaa.aod;

public final class BrightnessCurve {
    private BrightnessCurve() {}

    public static float targetForLux(float lux) {
        return targetForLux(lux, AodSettingsDefaults.balanced());
    }

    public static float targetForLux(float lux, AodSettingsSnapshot settings) {
        return AutomaticBrightnessProfile.targetFor(lux, settings);
    }

    public static float initialFromScreenBrightness(float brightness) {
        return initialFromScreenBrightness(brightness, AodSettingsDefaults.balanced());
    }

    public static float initialFromScreenBrightness(float brightness, AodSettingsSnapshot settings) {
        return AutomaticBrightnessProfile.initialFromScreenBrightness(brightness, settings);
    }
}
