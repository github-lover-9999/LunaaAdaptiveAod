package dev.lunaa.aod;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * System Framework part of "Keep AOD on in Battery Saver" (see {@link BatterySaverAod}).
 *
 * <p>{@code BatterySaverPolicy.getBatterySaverPolicy(int)} builds a fresh {@code PowerSaveState}
 * for each question. For AOD, while the option is on, the answer is replaced by a copy that no
 * longer blocks AOD; all other values are copied unchanged. Any failure keeps the stock answer.</p>
 */
final class BatterySaverAodHook {
    private static final String TAG = "LunaaAOD";
    private static final String BATTERY_SAVER_POLICY =
            "com.android.server.power.batterysaver.BatterySaverPolicy";

    private static boolean keptLogged;
    private static boolean failureLogged;

    private BatterySaverAodHook() {}

    static void install(ClassLoader classLoader) {
        Class<?> policyClass = XposedHelpers.findClass(BATTERY_SAVER_POLICY, classLoader);
        XposedHelpers.findAndHookMethod(
                policyClass,
                "getBatterySaverPolicy",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            afterGetBatterySaverPolicy(param);
                        } catch (Throwable t) {
                            logFailure(t);
                        }
                    }
                }
        );
    }

    private static void afterGetBatterySaverPolicy(XC_MethodHook.MethodHookParam param) throws Exception {
        if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof Integer)) return;
        int serviceType = (Integer) param.args[0];
        if (serviceType != BatterySaverAod.SERVICE_TYPE_AOD) return;
        Object state = param.getResult();
        if (state == null) return;
        boolean aodBlocked = (Boolean) XposedHelpers.getObjectField(state, "batterySaverEnabled");
        if (!BatterySaverAod.overrides(serviceType, aodBlocked, isAodKeptInBatterySaver())) return;

        param.setResult(copyWithAodAllowed(state));
        if (!keptLogged) {
            keptLogged = true;
            Log.i(TAG, "Battery Saver keeps AOD on (disable_aod answered false)");
            XposedBridge.log(TAG + ": Battery Saver keeps AOD on");
        }
    }

    private static boolean isAodKeptInBatterySaver() {
        XposedSettingsReader reader = SystemServerHooks.settings();
        synchronized (reader) {
            reader.reload();
            return reader.isAodKeptInBatterySaver();
        }
    }

    /** A new PowerSaveState from its Builder: AOD no longer blocked, all other values kept. */
    private static Object copyWithAodAllowed(Object state) throws Exception {
        Class<?> builderClass = Class.forName(state.getClass().getName() + "$Builder", false,
                state.getClass().getClassLoader());
        Object builder = builderClass.getDeclaredConstructor().newInstance();
        XposedHelpers.callMethod(builder, "setBatterySaverEnabled", false);
        XposedHelpers.callMethod(builder, "setGlobalBatterySaverEnabled",
                XposedHelpers.getObjectField(state, "globalBatterySaverEnabled"));
        XposedHelpers.callMethod(builder, "setLocationMode",
                XposedHelpers.getObjectField(state, "locationMode"));
        XposedHelpers.callMethod(builder, "setSoundTriggerMode",
                XposedHelpers.getObjectField(state, "soundTriggerMode"));
        XposedHelpers.callMethod(builder, "setBrightnessFactor",
                XposedHelpers.getObjectField(state, "brightnessFactor"));
        return XposedHelpers.callMethod(builder, "build");
    }

    private static synchronized void logFailure(Throwable t) {
        if (failureLogged) return;
        failureLogged = true;
        String message = "Battery Saver AOD hook failed; Battery Saver keeps its stock AOD policy";
        Log.e(TAG, message, t);
        XposedBridge.log(TAG + ": " + message);
        XposedBridge.log(t);
    }
}
