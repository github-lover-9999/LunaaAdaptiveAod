package dev.lunaa.aod;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

public final class AodReleaseInfo {
    public static final String VERSION_NAME = "1.6.7";
    public static final int VERSION_CODE = 16700;
    public static final String VERSION_TAG = "v1.6.7";

    public static String getInstalledVersionName(Context context) {
        if (context != null) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (info != null && info.versionName != null && !info.versionName.isEmpty()) {
                    return info.versionName;
                }
            } catch (Throwable ignored) {}
        }
        return VERSION_NAME;
    }

    public static int getInstalledVersionCode(Context context) {
        if (context != null) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (info != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        return (int) info.getLongVersionCode();
                    } else {
                        return info.versionCode;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return VERSION_CODE;
    }

    private AodReleaseInfo() {}
}