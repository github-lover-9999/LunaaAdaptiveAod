package dev.lunaa.aod;

import android.util.Log;

import de.robv.android.xposed.XposedHelpers;

public final class DozeBridge {
    private static final String TAG = "LunaaAOD";

    private Object cachedService;

    public boolean applyBrightness(Object dozeScreenBrightness, float target) {
        if (dozeScreenBrightness == null || Float.isNaN(target) || Float.isInfinite(target)) {
            return false;
        }
        try {
            Object service = cachedService;
            if (service == null) {
                service = XposedHelpers.getObjectField(dozeScreenBrightness, "mDozeService");
                cachedService = service;
            }
            if (service == null) {
                Log.w(TAG, "mDozeService is null; leaving stock brightness untouched");
                return false;
            }
            try {
                XposedHelpers.callMethod(service, "setDozeScreenBrightness", target);
                return true;
            } catch (Throwable t1) {
                int intBrightness = Math.round(target * 255f);
                XposedHelpers.callMethod(service, "setDozeScreenBrightness", intBrightness);
                return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to apply doze brightness; stock behavior retained", t);
            return false;
        }
    }
}
