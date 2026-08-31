package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class XposedSettingsWiringTest {
    @Test public void manifestEnablesSharedPrefsWithoutBroadeningScope() throws Exception {
        String manifest = read("app/src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("android:name=\"xposedsharedprefs\""));
        assertTrue(manifest.contains("android:value=\"true\""));
        assertTrue(manifest.contains("android:name=\"xposedscope\" android:value=\"com.android.systemui\""));
        assertFalse(manifest.contains("android:value=\"android\""));
    }

    @Test public void systemUiReadsModulePrefsThroughXSharedPreferences() throws Exception {
        String reader = read("app/src/main/java/dev/lunaa/aod/XposedSettingsReader.java");
        assertTrue(reader.contains("new XSharedPreferences(\"dev.lunaa.aod\", AodSettingsCodec.PREF_FILE)"));
        assertTrue(reader.contains("preferences.reload()"));
        assertTrue(reader.contains("AodSettingsCodec.readOrDefault"));
        assertTrue(reader.contains("settings revision="));
    }

    @Test public void runtimeFieldControllerReceivesSettingsReaderWithDisplayManagerContextForRootBridge() throws Exception {
        String hooks = read("app/src/main/java/dev/lunaa/aod/SystemUiHooks.java");
        String controller = read("app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java");

        assertTrue(hooks.contains("new XposedSettingsReader()"));
        assertTrue(controller.contains("XposedSettingsReader settingsReader"));
        assertTrue(controller.contains("this.settingsReader = settingsReader"));
        assertFalse(hooks.contains("ActivityThread"));
        assertTrue(hooks.contains("RuntimeFieldResolver.readExactOrUniqueAssignable(displayValue, \"mContext\", Context.class)"));
        assertFalse(hooks.contains("hookAllConstructors"));
    }

    private static String read(String path) throws Exception {
        return TestProjectFiles.read(path);
    }
}
