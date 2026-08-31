package dev.lunaa.aod;

import android.util.Log;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public final class XposedSettingsReader {
    private static final String TAG = "LunaaAOD";
    private final XSharedPreferences preferences;
    private int lastLoggedRevision = Integer.MIN_VALUE;
    private boolean failureLogged;

    public XposedSettingsReader() {
        preferences = new XSharedPreferences("dev.lunaa.aod", AodSettingsCodec.PREF_FILE);
    }

    public boolean isAutomaticExtraBrightnessEnabled() {
        try {
            return preferences.getBoolean(
                    AodSettingsCodec.KEY_AUTOMATIC_EXTRA_BRIGHT_ENABLED,
                    true
            );
        } catch (Throwable t) {
            return true;
        }
    }

    public AodSettingsSnapshot reload() {
        try {
            preferences.reload();
            AodSettingsSnapshot snapshot = AodSettingsCodec.readOrDefault(
                    new AodSettingsCodec.Reader() {
                        @Override
                        public boolean getBoolean(String key, boolean fallback) {
                            return preferences.getBoolean(key, fallback);
                        }

                        @Override
                        public int getInt(String key, int fallback) {
                            return preferences.getInt(key, fallback);
                        }

                        @Override
                        public float getFloat(String key, float fallback) {
                            return preferences.getFloat(key, fallback);
                        }
                    }
            );
            if (snapshot.getRevision() != lastLoggedRevision) {
                lastLoggedRevision = snapshot.getRevision();
                String message = "settings revision=" + snapshot.getRevision()
                        + " mode=" + (snapshot.isAutomaticMode() ? "AUTO" : "MANUAL")
                        + " preset=" + snapshot.getPreset()
                        + " cap=" + snapshot.getAutomaticCapPercent() + "%"
                        + " manual=" + snapshot.getManualBrightness();
                Log.i(TAG, message);
                XposedBridge.log(TAG + ": " + message);
            }
            failureLogged = false;
            return snapshot;
        } catch (Throwable t) {
            if (!failureLogged) {
                failureLogged = true;
                Log.e(TAG, "Could not read module settings; leaving stock behavior active", t);
                XposedBridge.log(TAG + ": settings read failed; leaving stock behavior active");
                XposedBridge.log(t);
            }
            return AodSettingsDefaults.disabledBalanced();
        }
    }
}
