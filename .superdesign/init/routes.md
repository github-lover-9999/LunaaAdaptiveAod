# Routes / Android entry points

- Launcher activity: `dev.lunaa.aod.SettingsActivity`
- Xposed entry: `dev.lunaa.aod.LunaaAodModule` via `assets/xposed_init`
- SystemUI scope only; the launcher activity is only the settings surface.

## AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="false"
        android:description="@string/app_description"
        android:hasCode="true"
        android:label="@string/app_name"
        android:supportsRtl="true">
        <activity
            android:name=".SettingsActivity"
            android:exported="true"
            android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <meta-data android:name="xposedmodule" android:value="true" />
        <meta-data android:name="xposeddescription" android:value="@string/app_description" />
        <meta-data android:name="xposedminversion" android:value="82" />
        <meta-data android:name="xposedsharedprefs" android:value="true" />
        <meta-data android:name="xposedscope" android:value="com.android.systemui" />
    </application>
</manifest>

```
