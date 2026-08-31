package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RootBridgeSourceTest {
    @Test
    public void receiverUsesRuntimeSystemUiIdentityAndRaceSafeRootSequence() throws Exception {
        String receiver = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/RootHbmBridgeReceiver.java");
        String client = TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/RootHbmBridgeClient.java");
        String manifest = TestProjectFiles.read("app/src/main/AndroidManifest.xml");

        assertFalse(receiver.contains("Process.SYSTEM_UID"));
        assertTrue(receiver.contains("getSentFromUid()"));
        assertTrue(receiver.contains("getSentFromPackage()"));
        assertTrue(receiver.contains("getPackagesForUid(senderUid)"));
        assertTrue(receiver.contains("ApplicationInfo.FLAG_SYSTEM"));
        assertTrue(receiver.contains("RootCommandGate"));
        assertTrue(receiver.contains("newSingleThreadExecutor"));
        assertTrue(receiver.contains("COMMAND_GATE.isCurrent"));
        assertTrue(receiver.contains("runRootWrite(\"0\")"));
        assertTrue(receiver.contains("Thread.sleep(EDGE_DELAY_MS)"));
        assertTrue(receiver.contains("runRootWrite(\"1\")"));
        assertTrue(receiver.contains("TimeUnit.MILLISECONDS"));
        assertFalse(receiver.contains("sleep 0.10"));
        assertTrue(client.contains("setShareIdentityEnabled(true)"));
        assertTrue(client.contains("Intent.FLAG_RECEIVER_FOREGROUND"));
        assertTrue(client.contains("sendOrderedBroadcast"));
        assertTrue(manifest.contains("<queries>"));
        assertTrue(manifest.contains("android:name=\"com.android.systemui\""));
    }
}
