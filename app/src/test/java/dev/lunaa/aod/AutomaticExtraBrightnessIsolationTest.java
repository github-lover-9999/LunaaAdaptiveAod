package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AutomaticExtraBrightnessIsolationTest {
    @Test
    public void autoToggleIsPersistedOutsideCoreSnapshotAndCannotChangeManualPolicy() throws Exception {
        String snapshot = read("AodSettingsSnapshot.java");
        String codec = read("AodSettingsCodec.java");
        String store = read("AndroidSettingsStore.java");
        String reader = read("XposedSettingsReader.java");
        String adaptive = read("AdaptiveAodController.java");
        String policy = read("ExtraBrightnessPolicy.java");

        assertFalse(snapshot.contains("automaticExtraBright"));
        assertTrue(codec.contains("KEY_AUTOMATIC_EXTRA_BRIGHT_ENABLED"));
        assertTrue(store.contains("loadAutomaticExtraBrightnessEnabled"));
        assertTrue(store.contains("saveAutomaticExtraBrightnessEnabled"));
        assertTrue(reader.contains("isAutomaticExtraBrightnessEnabled"));
        assertTrue(adaptive.contains("automaticExtraBrightnessEnabled"));
        assertTrue(adaptive.contains("currentSettings.isAutomaticMode() && !automaticExtraBrightnessEnabled"));
        assertFalse("Manual HBM policy must remain identical to the working v1.6.4j path",
                policy.contains("AutomaticExtra"));
    }

    @Test
    public void automaticUiHasToggleButNoAutoHbmCaption() throws Exception {
        String activity = read("SettingsActivity.java");
        String strings = TestProjectFiles.read("app/src/main/res/values/strings.xml");
        String manifest = TestProjectFiles.read("app/src/main/AndroidManifest.xml");

        assertTrue(activity.contains("automaticExtraBrightnessSwitch"));
        assertTrue(activity.contains("Enable Extra Bright (HBM)"));
        assertFalse(activity.contains("Auto HBM"));
        assertFalse(strings.contains(">Auto HBM<"));
        assertTrue(manifest.contains("android:icon=\"@drawable/icon\""));
    }

    private static String read(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
