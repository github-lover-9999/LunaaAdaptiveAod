package dev.lunaa.aod;

public enum AodPreset {
    DIM(0, 0, 40, 25),
    BALANCED(1, 40, 100, 100),
    BRIGHT(2, 70, 100, 100),
    CUSTOM(3, 0, 100, 80);

    private static final float TOLERANCE = 0.0005f;

    private final int persistedValue;
    private final int minCapPercent;
    private final int maxCapPercent;
    private final int defaultCapPercent;

    AodPreset(int persistedValue, int minCapPercent, int maxCapPercent, int defaultCapPercent) {
        this.persistedValue = persistedValue;
        this.minCapPercent = minCapPercent;
        this.maxCapPercent = maxCapPercent;
        this.defaultCapPercent = defaultCapPercent;
    }

    public int persistedValue() {
        return persistedValue;
    }

    public int minCapPercent() {
        return minCapPercent;
    }

    public int maxCapPercent() {
        return maxCapPercent;
    }

    public int defaultCapPercent() {
        return defaultCapPercent;
    }

    public boolean acceptsCapPercent(int value) {
        return value >= minCapPercent && value <= maxCapPercent;
    }

    public static AodPreset fromPersistedValue(int value) {
        for (AodPreset preset : values()) {
            if (preset.persistedValue == value) return preset;
        }
        return BALANCED;
    }

    public static AodPreset detect(AodSettingsSnapshot snapshot) {
        if (snapshot == null) return CUSTOM;
        if (matches(snapshot, AodSettingsDefaults.dim())) return DIM;
        if (matches(snapshot, AodSettingsDefaults.balanced())) return BALANCED;
        if (matches(snapshot, AodSettingsDefaults.bright())) return BRIGHT;
        return CUSTOM;
    }

    private static boolean matches(AodSettingsSnapshot actual, AodSettingsSnapshot preset) {
        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            if (Math.abs(actual.luxAt(i) - preset.luxAt(i)) > TOLERANCE) return false;
            if (Math.abs(actual.brightnessAt(i) - preset.brightnessAt(i)) > TOLERANCE) return false;
        }
        return true;
    }
}
