package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RootBridgeSenderPolicyTest {
    @Test
    public void acceptsSystemUiWithRuntimeAppUid() {
        assertTrue(RootBridgeSenderPolicy.isTrusted(
                10301,
                "com.android.systemui",
                new String[]{"com.android.systemui"},
                true));
    }

    @Test
    public void rejectsSpoofedOrNonSystemSenders() {
        assertFalse(RootBridgeSenderPolicy.isTrusted(
                10301,
                "evil.example",
                new String[]{"com.android.systemui"},
                true));
        assertFalse(RootBridgeSenderPolicy.isTrusted(
                10301,
                "com.android.systemui",
                new String[]{"other.package"},
                true));
        assertFalse(RootBridgeSenderPolicy.isTrusted(
                10301,
                "com.android.systemui",
                new String[]{"com.android.systemui"},
                false));
    }

    @Test
    public void acceptsSystemUiWithSystemUid1000() {
        // Edge case: some ROMs may run SystemUI under the system UID.
        assertTrue(RootBridgeSenderPolicy.isTrusted(
                1000,
                "com.android.systemui",
                new String[]{"com.android.systemui", "android"},
                true));
    }

    @Test
    public void rejectsNegativeUid() {
        // If PackageManager.getPackageUid() threw NameNotFoundException, senderUid = -1.
        assertFalse(RootBridgeSenderPolicy.isTrusted(
                -1,
                "com.android.systemui",
                new String[]{"com.android.systemui"},
                true));
    }
}
