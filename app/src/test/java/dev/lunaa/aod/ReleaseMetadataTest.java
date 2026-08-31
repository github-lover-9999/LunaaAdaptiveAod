package dev.lunaa.aod;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReleaseMetadataTest {
    @Test
    public void releaseFilesIdentifyCurrentVersion() throws Exception {
        String gradle = read("app/build.gradle.kts");
        String buildScript = read("tools/build-windows.ps1");
        String readme = read("README.md");

        assertTrue(gradle.contains("versionCode = 16500"));
        assertTrue(gradle.contains("versionName = \"1.6.5\""));
        assertTrue(buildScript.contains("LunaaAdaptiveAod-v1.6.4m-build.apk"));
        assertTrue(readme.contains("Lunaa Adaptive AOD"));
    }

    @Test
    public void docsDescribeKeyFeatures() throws Exception {
        String readme = read("README.md");
        String qa = read("QA.md");

        assertTrue(readme.contains("DIM"));
        assertTrue(readme.contains("BALANCED"));
        assertTrue(readme.contains("BRIGHT"));
        assertTrue(readme.contains("LSPosed"));
        assertTrue(readme.contains("KernelSU"));
        assertTrue(readme.contains("In-App GitHub Auto-Updater"));

        assertTrue(qa.contains("notify_fppress"));
        assertTrue(qa.contains("AppUpdaterTest"));
    }

    @Test
    public void docsKeepRuntimeFieldArchitectureAndSystemUiScope() throws Exception {
        String readme = read("README.md");
        String qa = read("QA.md");

        assertTrue(readme.contains("transitionTo"));
        assertTrue(readme.contains("RuntimeFieldResolver"));
        assertTrue(readme.contains("com.android.systemui"));

        assertTrue(qa.contains("controller attached source=runtime-fields"));
    }

    @Test
    public void licenseAndGitignoreAreConfigured() throws Exception {
        String license = read("LICENSE");
        String gitignore = read(".gitignore");

        assertTrue(license.contains("GNU GENERAL PUBLIC LICENSE"));
        assertTrue(license.contains("Version 3"));
        assertTrue(gitignore.contains("*.keystore"));
    }

    private static String read(String path) throws Exception {
        return TestProjectFiles.read(path);
    }
}
