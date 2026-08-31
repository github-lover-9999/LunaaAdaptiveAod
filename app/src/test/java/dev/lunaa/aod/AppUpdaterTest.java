package dev.lunaa.aod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AppUpdaterTest {

    @Test
    public void versionComparisonDetectsNewerMajorAndMinorAndSuffix() {
        assertTrue(AppUpdater.isNewerVersion("1.6.4", 16483, "v1.6.5", ""));
        assertTrue(AppUpdater.isNewerVersion("1.6.4r", 16482, "v1.6.4s", ""));
        assertTrue(AppUpdater.isNewerVersion("1.6.4s", 16483, "v1.7.0", ""));
        assertTrue(AppUpdater.isNewerVersion("1.6.4s", 16483, "v2.0.0", ""));

        assertFalse(AppUpdater.isNewerVersion("1.6.4s", 16483, "v1.6.4s", ""));
        assertFalse(AppUpdater.isNewerVersion("1.6.4s", 16483, "v1.6.4r", ""));
        assertFalse(AppUpdater.isNewerVersion("1.6.4s", 16483, "v1.6.3", ""));
    }

    @Test
    public void parseReleaseJsonExtractsApkAndMetadata() {
        String json = "{\n" +
                "  \"tag_name\": \"v1.6.5\",\n" +
                "  \"name\": \"Lunaa Adaptive AOD v1.6.5\",\n" +
                "  \"body\": \"## What's Changed\\n- Better brightness\\n- Auto updater\",\n" +
                "  \"assets\": [\n" +
                "    {\n" +
                "      \"name\": \"LunaaAdaptiveAod-v1.6.5-signed.apk\",\n" +
                "      \"browser_download_url\": \"https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.5/LunaaAdaptiveAod-v1.6.5-signed.apk\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        AppUpdater.ReleaseInfo info = AppUpdater.parseReleaseJson(json, "1.6.4s", 16483);
        assertNotNull(info);
        assertEquals("v1.6.5", info.tagName);
        assertEquals("Lunaa Adaptive AOD v1.6.5", info.title);
        assertEquals("LunaaAdaptiveAod-v1.6.5-signed.apk", info.apkName);
        assertEquals("https://github.com/github-lover-9999/LunaaAdaptiveAod/releases/download/v1.6.5/LunaaAdaptiveAod-v1.6.5-signed.apk", info.apkDownloadUrl);
        assertTrue(info.hasUpdate);
    }
}