package dev.lunaa.aod;

import java.util.Locale;

/** Pure identity gate for the Realme GT Master Edition (lunaa) family. */
public final class LunaaDevicePolicy {
    private LunaaDevicePolicy() {}

    public static boolean isSupportedIdentity(
            String device,
            String product,
            String model,
            String manufacturer
    ) {
        String d = normalize(device);
        String p = normalize(product);
        String m = normalize(model);
        String maker = normalize(manufacturer);

        boolean lunaa = containsToken(d, "lunaa") || containsToken(p, "lunaa");
        boolean rmx336Family = d.contains("rmx336") || p.contains("rmx336") || m.contains("rmx336");
        boolean oplusBoard = d.contains("re54ab") || p.contains("re54ab") || m.contains("re54ab");
        boolean realmeIdentity = maker.contains("realme") || m.contains("realme");
        boolean gtMasterIdentity = m.contains("gt master edition") || m.contains("gt master");

        // Custom ROMs may rewrite Build.DEVICE / Build.PRODUCT while preserving the
        // retail RMX336x model identity or RE54ABL1 project ID. The RMX336 and RE54AB
        // families are specific to the GT Master Edition panel path.
        return rmx336Family || oplusBoard || (lunaa && realmeIdentity) || (gtMasterIdentity && realmeIdentity);
    }

    private static boolean containsToken(String value, String token) {
        if (value.equals(token)) return true;
        return value.startsWith(token + "_")
                || value.endsWith("_" + token)
                || value.contains("_" + token + "_")
                || value.startsWith(token + "-")
                || value.endsWith("-" + token)
                || value.contains("-" + token + "-");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
