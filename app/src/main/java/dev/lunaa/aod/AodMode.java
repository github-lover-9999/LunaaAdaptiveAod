package dev.lunaa.aod;

public enum AodMode {
    AUTOMATIC(0),
    MANUAL(1);

    private final int persistedValue;

    AodMode(int persistedValue) {
        this.persistedValue = persistedValue;
    }

    public int persistedValue() {
        return persistedValue;
    }

    public static AodMode fromPersistedValue(int value) {
        for (AodMode mode : values()) {
            if (mode.persistedValue == value) return mode;
        }
        throw new IllegalArgumentException("unknown AOD mode: " + value);
    }
}
