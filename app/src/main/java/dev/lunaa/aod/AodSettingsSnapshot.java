package dev.lunaa.aod;

import java.util.Arrays;

public final class AodSettingsSnapshot {
    public static final int POINT_COUNT = 9;
    public static final float MIN_BRIGHTNESS = 0.010f;
    public static final float MAX_BRIGHTNESS = 1.000f;
    public static final int MIN_MULTIPLIER_PERCENT = 50;
    public static final int MAX_MULTIPLIER_PERCENT = 300;

    private final boolean enabled;
    private final AodMode mode;
    private final int multiplierPercent;
    private final float minimumAutoBrightness;
    private final float manualBrightness;
    private final AodPreset preset;
    private final int dimCapPercent;
    private final int balancedCapPercent;
    private final int brightCapPercent;
    private final int extraBrightPercent;
    private final boolean manualExtraBrightEnabled;
    private final BrightnessLevelConfig manualLevels;
    private final BrightnessLevelConfig extraBrightLevels;
    private final float[] lux;
    private final float[] brightness;
    private final int revision;

    /** Compatibility constructor used by older call sites. Manual Extra Bright defaults off. */
    public AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            AodPreset preset,
            int dimCapPercent,
            int balancedCapPercent,
            int brightCapPercent,
            int manualLevel,
            int manualLevel1Percent,
            int manualLevel2Percent,
            int manualLevel3Percent,
            int extraBrightLevel,
            int extraLevel1Percent,
            int extraLevel2Percent,
            int extraLevel3Percent,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(enabled, mode, multiplierPercent, minimumAutoBrightness, preset,
                dimCapPercent, balancedCapPercent, brightCapPercent,
                manualLevel, manualLevel1Percent, manualLevel2Percent, manualLevel3Percent,
                false,
                extraBrightLevel, extraLevel1Percent, extraLevel2Percent, extraLevel3Percent,
                lux, brightness, revision);
    }

    /** Canonical constructor with explicit Manual Extra Bright state. */
    public AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            AodPreset preset,
            int dimCapPercent,
            int balancedCapPercent,
            int brightCapPercent,
            int manualLevel,
            int manualLevel1Percent,
            int manualLevel2Percent,
            int manualLevel3Percent,
            boolean manualExtraBrightEnabled,
            int extraBrightLevel,
            int extraLevel1Percent,
            int extraLevel2Percent,
            int extraLevel3Percent,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(
                enabled,
                mode,
                multiplierPercent,
                minimumAutoBrightness,
                new BrightnessLevelConfig(
                        manualLevel,
                        manualLevel1Percent,
                        manualLevel2Percent,
                        manualLevel3Percent),
                preset,
                dimCapPercent,
                balancedCapPercent,
                brightCapPercent,
                manualExtraBrightEnabled,
                new BrightnessLevelConfig(
                        extraBrightLevel,
                        extraLevel1Percent,
                        extraLevel2Percent,
                        extraLevel3Percent),
                lux,
                brightness,
                revision
        );
    }

    private AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            BrightnessLevelConfig manualLevels,
            AodPreset preset,
            int dimCapPercent,
            int balancedCapPercent,
            int brightCapPercent,
            boolean manualExtraBrightEnabled,
            BrightnessLevelConfig extraBrightLevels,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(
                enabled,
                mode,
                multiplierPercent,
                minimumAutoBrightness,
                manualLevels.getSelectedPercent() / 100f,
                preset,
                dimCapPercent,
                balancedCapPercent,
                brightCapPercent,
                extraBrightLevels.getSelectedPercent(),
                manualExtraBrightEnabled,
                manualLevels,
                extraBrightLevels,
                lux,
                brightness,
                revision
        );
    }

    /** Legacy-compatible constructor retained for tests/migrations and older call sites. */
    public AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            float manualBrightness,
            AodPreset preset,
            int dimCapPercent,
            int balancedCapPercent,
            int brightCapPercent,
            int extraBrightPercent,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(
                enabled,
                mode,
                multiplierPercent,
                minimumAutoBrightness,
                manualBrightness,
                preset,
                dimCapPercent,
                balancedCapPercent,
                brightCapPercent,
                extraBrightPercent,
                false,
                legacyManualLevels(manualBrightness),
                legacyExtraLevels(extraBrightPercent),
                lux,
                brightness,
                revision
        );
    }

    private AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            float manualBrightness,
            AodPreset preset,
            int dimCapPercent,
            int balancedCapPercent,
            int brightCapPercent,
            int extraBrightPercent,
            boolean manualExtraBrightEnabled,
            BrightnessLevelConfig manualLevels,
            BrightnessLevelConfig extraBrightLevels,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        if (mode == null) throw new IllegalArgumentException("mode is required");
        if (preset == null) throw new IllegalArgumentException("preset is required");
        if (manualLevels == null) throw new IllegalArgumentException("manualLevels are required");
        if (extraBrightLevels == null) throw new IllegalArgumentException("extraBrightLevels are required");
        if (multiplierPercent < MIN_MULTIPLIER_PERCENT
                || multiplierPercent > MAX_MULTIPLIER_PERCENT) {
            throw new IllegalArgumentException("multiplierPercent must be 50..300");
        }
        validateBrightness("minimumAutoBrightness", minimumAutoBrightness);
        validateBrightness("manualBrightness", manualBrightness);
        validateCap(AodPreset.DIM, dimCapPercent, "dimCapPercent");
        validateCap(AodPreset.BALANCED, balancedCapPercent, "balancedCapPercent");
        validateCap(AodPreset.BRIGHT, brightCapPercent, "brightCapPercent");
        if (extraBrightPercent < ExtraBrightnessLevel.MIN_PERCENT
                || extraBrightPercent > ExtraBrightnessLevel.MAX_PERCENT) {
            throw new IllegalArgumentException("extraBrightPercent must be 1..100");
        }
        if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
        if (lux == null || brightness == null
                || lux.length != POINT_COUNT || brightness.length != POINT_COUNT) {
            throw new IllegalArgumentException("exactly 9 curve points are required");
        }

        float previousLux = -1f;
        for (int i = 0; i < POINT_COUNT; i++) {
            float luxValue = lux[i];
            float brightnessValue = brightness[i];
            if (!Float.isFinite(luxValue) || luxValue < 0f) {
                throw new IllegalArgumentException("lux[" + i + "] must be finite and >= 0");
            }
            if (i > 0 && luxValue <= previousLux) {
                throw new IllegalArgumentException("lux values must be strictly increasing");
            }
            validateBrightness("brightness[" + i + "]", brightnessValue);
            previousLux = luxValue;
        }

        this.enabled = enabled;
        this.mode = mode;
        this.multiplierPercent = multiplierPercent;
        this.minimumAutoBrightness = minimumAutoBrightness;
        this.manualBrightness = manualBrightness;
        this.preset = preset;
        this.dimCapPercent = dimCapPercent;
        this.balancedCapPercent = balancedCapPercent;
        this.brightCapPercent = brightCapPercent;
        this.extraBrightPercent = extraBrightPercent;
        this.manualExtraBrightEnabled = manualExtraBrightEnabled;
        this.manualLevels = manualLevels;
        this.extraBrightLevels = extraBrightLevels;
        this.lux = Arrays.copyOf(lux, lux.length);
        this.brightness = Arrays.copyOf(brightness, brightness.length);
        this.revision = revision;
    }

    public AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            float manualBrightness,
            AodPreset preset,
            int dimCapPercent,
            int balancedCapPercent,
            int brightCapPercent,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(enabled, mode, multiplierPercent, minimumAutoBrightness, manualBrightness,
                preset, dimCapPercent, balancedCapPercent, brightCapPercent,
                ExtraBrightnessLevel.DEFAULT_PERCENT, lux, brightness, revision);
    }

    public AodSettingsSnapshot(
            boolean enabled,
            AodMode mode,
            int multiplierPercent,
            float minimumAutoBrightness,
            float manualBrightness,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(
                enabled,
                mode,
                multiplierPercent,
                minimumAutoBrightness,
                manualBrightness,
                AodPreset.BALANCED,
                AodPreset.DIM.defaultCapPercent(),
                AodPreset.BALANCED.defaultCapPercent(),
                AodPreset.BRIGHT.defaultCapPercent(),
                lux,
                brightness,
                revision
        );
    }

    public AodSettingsSnapshot(
            boolean enabled,
            int multiplierPercent,
            float[] lux,
            float[] brightness,
            int revision
    ) {
        this(
                enabled,
                AodMode.AUTOMATIC,
                multiplierPercent,
                AodSettingsDefaults.DEFAULT_MINIMUM_AUTO_BRIGHTNESS,
                AodSettingsDefaults.DEFAULT_MANUAL_BRIGHTNESS,
                lux,
                brightness,
                revision
        );
    }

    public boolean isEnabled() { return enabled; }
    public AodMode getMode() { return mode; }
    public boolean isAutomaticMode() { return mode == AodMode.AUTOMATIC; }
    public boolean isManualMode() { return mode == AodMode.MANUAL; }
    public int getMultiplierPercent() { return multiplierPercent; }
    public float getMinimumAutoBrightness() { return minimumAutoBrightness; }
    public float getManualBrightness() { return manualBrightness; }
    public int getManualLevel() { return manualLevels.getSelectedLevel(); }
    public int getManualLevelPercent(int level) { return manualLevels.getPercent(level); }
    public boolean isManualExtraBrightEnabled() { return manualExtraBrightEnabled; }
    public AodPreset getPreset() { return preset; }
    public int getDimCapPercent() { return dimCapPercent; }
    public int getBalancedCapPercent() { return balancedCapPercent; }
    public int getBrightCapPercent() { return brightCapPercent; }
    public int getExtraBrightPercent() { return extraBrightPercent; }
    public int getExtraBrightLevel() { return extraBrightLevels.getSelectedLevel(); }
    public int getExtraBrightLevelPercent(int level) { return extraBrightLevels.getPercent(level); }

    public int getAutomaticCapPercent() {
        if (preset == AodPreset.DIM) return dimCapPercent;
        if (preset == AodPreset.BRIGHT) return brightCapPercent;
        return balancedCapPercent;
    }

    public int getRevision() { return revision; }
    public float luxAt(int index) { return lux[index]; }
    public float brightnessAt(int index) { return brightness[index]; }
    public float[] copyLux() { return Arrays.copyOf(lux, lux.length); }
    public float[] copyBrightness() { return Arrays.copyOf(brightness, brightness.length); }

    private static BrightnessLevelConfig legacyManualLevels(float brightness) {
        int percent = Math.max(1, Math.min(100, Math.round(brightness * 100f)));
        int level1 = AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_1_PERCENT;
        int level2 = AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_2_PERCENT;
        int level3 = AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_3_PERCENT;
        int level;
        if (percent >= 100) {
            level = 3;
        } else if (percent <= level1) {
            level = 1;
            level1 = percent;
        } else {
            level = 2;
            level2 = percent;
        }
        return new BrightnessLevelConfig(level, level1, level2, level3);
    }

    private static BrightnessLevelConfig legacyExtraLevels(int percent) {
        int safe = ExtraBrightnessLevel.normalize(percent);
        int level1 = AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_1_PERCENT;
        int level2 = AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_2_PERCENT;
        int level3 = AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_3_PERCENT;
        int level;
        if (safe >= 100) {
            level = 3;
        } else if (safe <= level1) {
            level = 1;
            level1 = safe;
        } else {
            level = 2;
            level2 = safe;
        }
        return new BrightnessLevelConfig(level, level1, level2, level3);
    }

    private static void validateCap(AodPreset preset, int value, String name) {
        if (!preset.acceptsCapPercent(value)) {
            throw new IllegalArgumentException(
                    name + " must be " + preset.minCapPercent() + ".." + preset.maxCapPercent()
            );
        }
    }

    private static void validateBrightness(String name, float value) {
        if (!Float.isFinite(value)
                || value < MIN_BRIGHTNESS
                || value > MAX_BRIGHTNESS) {
            throw new IllegalArgumentException(name + " must be 0.010..1.000");
        }
    }
}
