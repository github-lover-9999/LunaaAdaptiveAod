package dev.lunaa.aod;

import static org.junit.Assert.*;
import org.junit.Test;

public class HbmCapabilityProbeSourceTest {
    @Test public void rootReceiverMustProbeIdentityAndAllProvenNodesBeforeAnyHbmWrite() throws Exception {
        String receiver = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/RootHbmBridgeReceiver.java");
        String probe = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/HbmCapabilityProbe.java");
        String identity = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/LunaaDevicePolicy.java");

        assertTrue(identity.toLowerCase(java.util.Locale.ROOT).contains("rmx336"));
        assertTrue(identity.contains("lunaa"));
        assertTrue(probe.contains("/sys/kernel/oplus_display/notify_fppress"));
        assertTrue(probe.contains("/sys/kernel/oplus_display/dimlayer_hbm"));
        assertTrue(probe.contains("/sys/class/backlight/panel0-backlight/brightness"));
        assertTrue(probe.contains("LunaaDevicePolicy.isSupportedIdentity"));

        int capability = receiver.indexOf("HbmCapabilityProbe.probeViaRoot");
        int firstWrite = receiver.indexOf("runRootWrite(");
        assertTrue("capability probe must exist", capability >= 0);
        assertTrue("capability probe must precede any root write call", capability < firstWrite);
    }
}
