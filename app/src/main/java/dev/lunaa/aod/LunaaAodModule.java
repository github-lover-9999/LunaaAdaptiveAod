package dev.lunaa.aod;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class LunaaAodModule implements IXposedHookLoadPackage {
    private static final String TAG = "LunaaAOD";
    private static final String SYSTEM_UI = "com.android.systemui";
    /** Package and process name Xposed reports for system_server (System Framework scope). */
    private static final String SYSTEM_FRAMEWORK = "android";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null) return;
        if (SYSTEM_FRAMEWORK.equals(lpparam.packageName)
                && SYSTEM_FRAMEWORK.equals(lpparam.processName)) {
            try {
                SystemServerHooks.install(lpparam.classLoader);
            } catch (Throwable t) {
                Log.e(TAG, "System Framework hooks failed; stock display ramp and Battery Saver retained", t);
                XposedBridge.log(t);
            }
            return;
        }
        if (!SYSTEM_UI.equals(lpparam.packageName) || !SYSTEM_UI.equals(lpparam.processName)) return;
        try {
            SystemUiHooks.install(lpparam.classLoader);
            Log.i(TAG, "hooks installed for " + lpparam.packageName + "/" + lpparam.processName);
            XposedBridge.log(TAG + ": hooks installed");
        } catch (Throwable t) {
            Log.e(TAG, "Hook installation failed; stock SystemUI behavior retained", t);
            XposedBridge.log(t);
        }
    }
}
