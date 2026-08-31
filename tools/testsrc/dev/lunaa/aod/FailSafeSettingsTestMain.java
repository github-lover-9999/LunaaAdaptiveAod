package dev.lunaa.aod;

public final class FailSafeSettingsTestMain {
    public static void main(String[] args) {
        AodSettingsSnapshot snapshot = AodSettingsCodec.readOrDefault(new AodSettingsCodec.Reader() {
            @Override public boolean getBoolean(String key, boolean fallback) { throw new IllegalStateException("corrupt prefs"); }
            @Override public int getInt(String key, int fallback) { throw new IllegalStateException("corrupt prefs"); }
            @Override public float getFloat(String key, float fallback) { throw new IllegalStateException("corrupt prefs"); }
        });
        if (snapshot.isEnabled()) {
            throw new AssertionError("settings decode failure must fail closed to stock behavior");
        }
        System.out.println("PASS FailSafeSettingsTestMain");
    }
}
