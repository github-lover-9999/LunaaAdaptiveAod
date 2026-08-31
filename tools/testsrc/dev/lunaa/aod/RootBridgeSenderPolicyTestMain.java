package dev.lunaa.aod;

public final class RootBridgeSenderPolicyTestMain {
    public static void main(String[] args) {
        yes(RootBridgeSenderPolicy.isTrusted(
                10301,
                "com.android.systemui",
                new String[]{"com.android.systemui"},
                true));

        no(RootBridgeSenderPolicy.isTrusted(
                10301,
                "evil.example",
                new String[]{"com.android.systemui"},
                true));

        no(RootBridgeSenderPolicy.isTrusted(
                10301,
                "com.android.systemui",
                new String[]{"other.package"},
                true));

        no(RootBridgeSenderPolicy.isTrusted(
                10301,
                "com.android.systemui",
                new String[]{"com.android.systemui"},
                false));

        no(RootBridgeSenderPolicy.isTrusted(
                -1,
                "com.android.systemui",
                new String[]{"com.android.systemui"},
                true));

        System.out.println("PASS RootBridgeSenderPolicyTestMain");
    }

    private static void yes(boolean value) {
        if (!value) throw new AssertionError("expected trusted sender");
    }

    private static void no(boolean value) {
        if (value) throw new AssertionError("expected rejected sender");
    }
}
