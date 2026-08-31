package dev.lunaa.aod;

final class RootBridgeSenderPolicy {
    private static final String SYSTEM_UI = "com.android.systemui";

    private RootBridgeSenderPolicy() {}

    static boolean isTrusted(
            int senderUid,
            String senderPackage,
            String[] packagesForUid,
            boolean systemUiIsSystemApp) {
        if (senderUid < 0 || !SYSTEM_UI.equals(senderPackage) || !systemUiIsSystemApp) {
            return false;
        }
        if (packagesForUid == null) {
            return false;
        }
        for (String packageName : packagesForUid) {
            if (SYSTEM_UI.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
