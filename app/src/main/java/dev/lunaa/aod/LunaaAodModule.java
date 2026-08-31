package dev.lunaa.aod;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class LunaaAodModule implements IXposedHookLoadPackage {
    private static final String TAG = "LunaaAOD";
    private static final String SYSTEM_UI = "com.android.systemui";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null
                || !SYSTEM_UI.equals(lpparam.packageName)
                || !SYSTEM_UI.equals(lpparam.processName)) return;
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
