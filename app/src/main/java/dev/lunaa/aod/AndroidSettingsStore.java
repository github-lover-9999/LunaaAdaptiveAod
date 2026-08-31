package dev.lunaa.aod;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

public final class AndroidSettingsStore {
    private final Context context;
    private final SharedPreferences preferences;
    private final boolean writableForXposed;

    @SuppressWarnings("deprecation")
    public AndroidSettingsStore(Context context) {
        this.context = context;
        SharedPreferences opened;
        try {
            opened = context.getSharedPreferences(
                    AodSettingsCodec.PREF_FILE,
                    Context.MODE_WORLD_READABLE
            );
        } catch (SecurityException unsupported) {
            opened = context.getSharedPreferences(
                    AodSettingsCodec.PREF_FILE,
                    Context.MODE_PRIVATE
            );
        }
        preferences = opened;
        writableForXposed = opened != null;
        makeReadable();
    }

    public boolean isWritableForXposed() {
        return writableForXposed;
    }

    private void makeReadable() {
        try {
            if (context != null && context.getApplicationInfo() != null && context.getApplicationInfo().dataDir != null) {
                File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
                if (prefsDir.exists()) {
                    prefsDir.setReadable(true, false);
                    prefsDir.setExecutable(true, false);
                }
                File prefFile = new File(prefsDir, AodSettingsCodec.PREF_FILE + ".xml");
                if (prefFile.exists()) {
                    prefFile.setReadable(true, false);
                }
            }
        } catch (Throwable ignored) {}
    }

    public AodSettingsSnapshot load() {
        return AodSettingsCodec.readOrDefault(new AodSettingsCodec.Reader() {
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
        });
    }

    public boolean loadAutomaticExtraBrightnessEnabled() {
        return preferences.getBoolean(
                AodSettingsCodec.KEY_AUTOMATIC_EXTRA_BRIGHT_ENABLED,
                true
        );
    }

    public boolean saveAutomaticExtraBrightnessEnabled(boolean enabled) {
        if (!writableForXposed) return false;
        boolean result = preferences.edit()
                .putBoolean(AodSettingsCodec.KEY_AUTOMATIC_EXTRA_BRIGHT_ENABLED, enabled)
                .commit();
        makeReadable();
        return result;
    }

    public boolean save(AodSettingsSnapshot snapshot) {
        if (!writableForXposed || snapshot == null) return false;
        final SharedPreferences.Editor editor = preferences.edit();
        boolean result = AodSettingsCodec.write(new AodSettingsCodec.Writer() {
            @Override
            public void putBoolean(String key, boolean value) {
                editor.putBoolean(key, value);
            }

            @Override
            public void putInt(String key, int value) {
                editor.putInt(key, value);
            }

            @Override
            public void putFloat(String key, float value) {
                editor.putFloat(key, value);
            }

            @Override
            public boolean commit() {
                return editor.commit();
            }
        }, snapshot);
        makeReadable();
        return result;
    }
}
