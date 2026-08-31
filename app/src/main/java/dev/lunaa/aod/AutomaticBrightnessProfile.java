package dev.lunaa.aod;

/**
 * Perceptual automatic-brightness envelope shared by all three presets.
 * The ambient response is normalized to 0..1; each preset maps that response
 * into a deliberately separated floor..cap range.
 */
public final class AutomaticBrightnessProfile {
    private static final float[] RESPONSE = {
            0.00f, 0.00f, 0.08f, 0.20f, 0.38f, 0.52f, 0.66f, 0.86f, 1.00f
    };

    private static final float DIM_FLOOR = 0.20f;
    private static final float BALANCED_FLOOR = 0.50f;
    private static final float BRIGHT_FLOOR = 1.00f;

    private AutomaticBrightnessProfile() {}

    public static float targetFor(float lux, AodSettingsSnapshot settings) {
        AodSettingsSnapshot effective = settings != null ? settings : AodSettingsDefaults.balanced();
        float normalized = normalizedForLux(lux, effective);
        return targetForNormalized(normalized, effective);
    }

    public static float initialFromScreenBrightness(float brightness, AodSettingsSnapshot settings) {
        AodSettingsSnapshot effective = settings != null ? settings : AodSettingsDefaults.balanced();
        float normalized;
        if (!Float.isFinite(brightness) || brightness < 0f) {
            normalized = 0f;
        } else {
            normalized = clamp(brightness * 0.60f, 0f, 0.45f);
        }
        return targetForNormalized(normalized, effective);
    }

    static float targetForNormalized(float normalized, AodSettingsSnapshot settings) {
        float cap = Math.max(
                AodSettingsSnapshot.MIN_BRIGHTNESS,
                settings.getAutomaticCapPercent() / 100f
        );
        float requestedFloor = floorFor(settings.getPreset());
        float floor = Math.min(cap, Math.max(AodSettingsSnapshot.MIN_BRIGHTNESS, requestedFloor));
        float t = clamp(normalized, 0f, 1f);
        return clamp(floor + t * (cap - floor), AodSettingsSnapshot.MIN_BRIGHTNESS, cap);
    }

    private static float normalizedForLux(float lux, AodSettingsSnapshot settings) {
        if (!Float.isFinite(lux) || lux < 0f) return 0f;
        if (lux <= settings.luxAt(0)) return RESPONSE[0];
        for (int i = 1; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            if (lux <= settings.luxAt(i)) {
                float lowerLux = settings.luxAt(i - 1);
                float upperLux = settings.luxAt(i);
                float ratio = (lux - lowerLux) / (upperLux - lowerLux);
                return RESPONSE[i - 1] + ratio * (RESPONSE[i] - RESPONSE[i - 1]);
            }
        }
        return RESPONSE[RESPONSE.length - 1];
    }

    private static float floorFor(AodPreset preset) {
        if (preset == AodPreset.DIM) return DIM_FLOOR;
        if (preset == AodPreset.BRIGHT) return BRIGHT_FLOOR;
        return BALANCED_FLOOR;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
