package de.robv.android.xposed;

/** Compile-only ABI stub. The runtime implementation is provided by Vector/Xposed. */
public final class XSharedPreferences {
    public XSharedPreferences(String packageName, String prefFileName) {}

    public void reload() {}

    public boolean getBoolean(String key, boolean fallback) {
        return fallback;
    }

    public int getInt(String key, int fallback) {
        return fallback;
    }

    public float getFloat(String key, float fallback) {
        return fallback;
    }
}
