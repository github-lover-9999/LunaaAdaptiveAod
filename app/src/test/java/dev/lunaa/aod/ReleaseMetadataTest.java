package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseMetadataTest {
    @Test
    public void releaseFilesIdentifyV164m() throws Exception {
        String gradle = read("app/build.gradle.kts");
        String buildScript = read("tools/build-windows.ps1");
        String readme = read("README.md");
        String qa = read("QA.md");

        assertTrue(gradle.contains("versionCode = 16483"));
        assertTrue(gradle.contains("versionName = \"1.6.4\""));
        assertTrue(buildScript.contains("LunaaAdaptiveAod-v1.6.4m-build.apk"));
        // README and QA still reference the architectural origin (l) for traceability
        assertTrue(readme.contains("Extra Bright"));
        assertTrue(qa.contains("Extra Bright"));
    }

    @Test
    public void docsDescribePresetCapsManualAndExtraBright() throws Exception {
        String readme = read("README.md");
        String qa = read("QA.md");

        assertTrue(readme.contains("Dim: 0–40%"));
        assertTrue(readme.contains("Balanced: 40–80%"));
        assertTrue(readme.contains("Bright: 70–100%"));
        assertTrue(readme.contains("Extra Bright"));
        assertTrue(readme.contains("1,500 lux"));
        assertTrue(readme.contains("700 lux"));
        assertTrue(readme.contains("Manual"));
        assertTrue(readme.contains("XSharedPreferences"));
        assertTrue(readme.contains("system bar insets"));
        assertTrue(readme.contains("32dp"));
        assertTrue(readme.contains("48dp"));

        assertTrue(qa.contains("settings revision="));
        assertTrue(qa.contains("mode=AUTO"));
        assertTrue(qa.contains("mode=MANUAL"));
        assertTrue(qa.contains("Extra Bright"));
        assertTrue(qa.contains("notify_fppress"));
        assertTrue(qa.contains("Stock fallback"));
    }

    @Test
    public void docsKeepRuntimeFieldArchitectureAndSystemUiOnlyScope() throws Exception {
        String readme = read("README.md");
        String qa = read("QA.md");

        assertTrue(readme.contains("transitionTo"));
        assertTrue(readme.contains("runtime-fields"));
        assertFalse(readme.contains("constructor hook"));
        assertTrue(readme.contains("DisplayManager.mContext"));
        assertTrue(readme.contains("android.sensor.light"));
        assertTrue(readme.contains("qti.sensor.lux_aod"));

        assertTrue(qa.contains("controller attached source=runtime-fields"));
        assertFalse(qa.contains("controller attached source=constructor"));
        assertFalse(qa.contains("controller attached source=mContext"));
        assertTrue(qa.contains("only `com.android.systemui`"));
        assertTrue(qa.contains("no UDFPS class hook"));
    }

    private static String read(String path) throws Exception {
        return TestProjectFiles.read(path);
    }
}
