package android.content;
public interface SharedPreferences {
    boolean getBoolean(String key, boolean defValue);
    int getInt(String key, int defValue);
    float getFloat(String key, float defValue);
    Editor edit();
    interface Editor {
        Editor putBoolean(String key, boolean value);
        Editor putInt(String key, int value);
        Editor putFloat(String key, float value);
        boolean commit();
    }
}
