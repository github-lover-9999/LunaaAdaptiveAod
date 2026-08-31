package dev.lunaa.aod;

import static org.junit.Assert.*;
import org.junit.Test;

public class QaCollectorSourceTest {
    @Test public void collectorCapturesIdentityRomPrefsAndCapabilityNodes() throws Exception {
        String script = TestProjectFiles.read("tools/collect-aod-qa.ps1");
        assertTrue(script.contains("ro.product.device"));
        assertTrue(script.contains("ro.product.product.device"));
        assertTrue(script.contains("ro.product.model"));
        assertTrue(script.contains("ro.product.manufacturer"));
        assertTrue(script.contains("ro.build.fingerprint"));
        assertTrue(script.contains("ro.crdroid.version"));
        assertTrue(script.contains("ro.axion.version"));
        assertTrue(script.contains("ro.lineage.version"));
        assertTrue(script.contains("aod_settings.xml"));
        assertTrue(script.contains("notify_fppress"));
        assertTrue(script.contains("dimlayer_hbm"));
        assertTrue(script.contains("panel0-backlight/brightness"));
        assertTrue(script.contains("device-"));
        assertTrue(script.contains("prefs-"));
        assertTrue(script.contains("capability-"));
    }

    @Test public void compatibilityNeverWritesGenericAxionHbmNode() throws Exception {
        String receiver = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/RootHbmBridgeReceiver.java");
        String probe = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/HbmCapabilityProbe.java");
        assertFalse(receiver.contains("hbm_mode"));
        assertFalse(probe.contains("hbm_mode"));
    }
}
