package dev.lunaa.aod;

import android.content.ContentResolver;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * System Framework part of the module.
 *
 * <p>AOD brightness changes without the display ramp: lunaa's display config ramps every
 * brightness change for up to 3 s (see {@link DozeRamp}), and SystemUI has no way to skip that for
 * the doze brightness. Before DisplayPowerController starts a ramp while its power policy is doze,
 * the rate becomes 0, which the ramp animator applies at once. The normal screen, including
 * unlock, keeps its ramp. Any failure leaves the stock ramp.</p>
 *
 * <p>Also installs {@link BatterySaverAodHook}; each hook works without the other.</p>
 */
public final class SystemServerHooks {
    private static final String TAG = "LunaaAOD";
    private static final String DISPLAY_POWER_CONTROLLER =
            "com.android.server.display.DisplayPowerController";

    private static boolean installAttempted;
    private static XposedSettingsReader settingsReader;
    private static Executor stampWriter;
    private static boolean jumpLogged;
    private static boolean failureLogged;

    private SystemServerHooks() {}

    public static synchronized void install(ClassLoader classLoader) {
        if (installAttempted) return;
        installAttempted = true;

        try {
            installDozeRampHook(classLoader);
            Log.i(TAG, "doze ramp hook installed in system_server");
            XposedBridge.log(TAG + ": doze ramp hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Doze ramp hook installation failed; stock display ramp retained", t);
            XposedBridge.log(TAG + ": doze ramp hook installation failed; stock display ramp retained");
            XposedBridge.log(t);
        }
        try {
            BatterySaverAodHook.install(classLoader);
            Log.i(TAG, "Battery Saver AOD hook installed in system_server");
            XposedBridge.log(TAG + ": Battery Saver AOD hook installed");
        } catch (Throwable t) {
            Log.e(TAG, "Battery Saver AOD hook installation failed; stock Battery Saver policy retained", t);
            XposedBridge.log(TAG + ": Battery Saver AOD hook installation failed");
            XposedBridge.log(t);
        }
    }

    private static void installDozeRampHook(ClassLoader classLoader) {
        Class<?> controllerClass = XposedHelpers.findClass(DISPLAY_POWER_CONTROLLER, classLoader);
        XposedHelpers.findAndHookMethod(
                controllerClass,
                "animateScreenBrightness",
                float.class, float.class, float.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            beforeAnimateScreenBrightness(param);
                        } catch (Throwable t) {
                            logFailure("doze ramp hook failed; stock display ramp retained", t);
                        }
                    }
                }
        );
    }

    private static void beforeAnimateScreenBrightness(XC_MethodHook.MethodHookParam param) {
        int policy = readPolicy(param.thisObject);
        if (policy != InstantDozeRamp.POLICY_DOZE || param.args == null || param.args.length < 3
                || !(param.args[2] instanceof Float)) return;
        boolean enabled = isModuleEnabled();
        if (!InstantDozeRamp.applies(policy, enabled)) return;

        float rate = (Float) param.args[2];
        param.args[2] = InstantDozeRamp.rampRate(policy, rate, enabled);
        long nowMs = SystemClock.elapsedRealtime();
        publishStamp(param.thisObject, nowMs);
        if (!jumpLogged) {
            jumpLogged = true;
            String message = "doze brightness applied without the display ramp (was rate=" + rate
                    + " target=" + param.args[0] + ")";
            Log.i(TAG, message);
            XposedBridge.log(TAG + ": " + message);
        }
    }

    /** {@code mPowerRequest.policy}, or {@link InstantDozeRamp#UNKNOWN_POLICY} before the first request. */
    private static int readPolicy(Object displayPowerController) {
        Object request = XposedHelpers.getObjectField(displayPowerController, "mPowerRequest");
        Object policy = request == null ? null : XposedHelpers.getObjectField(request, "policy");
        return policy instanceof Integer ? (Integer) policy : InstantDozeRamp.UNKNOWN_POLICY;
    }

    /** The module's AOD switch: off means the stock AOD, ramp included. */
    private static boolean isModuleEnabled() {
        XposedSettingsReader reader = settings();
        synchronized (reader) {
            return reader.reload().isEnabled();
        }
    }

    /** Module settings as seen from system_server; callers synchronize on it. */
    static synchronized XposedSettingsReader settings() {
        if (settingsReader == null) settingsReader = new XposedSettingsReader();
        return settingsReader;
    }

    /** Written off the display thread; SystemUI reads it with {@link InstantRampProbe}. */
    private static void publishStamp(Object displayPowerController, long nowMs) {
        Object context = XposedHelpers.getObjectField(displayPowerController, "mContext");
        if (!(context instanceof Context)) return;
        ContentResolver resolver = ((Context) context).getContentResolver();
        stampWriter().execute(() -> {
            try {
                Settings.Global.putLong(resolver, InstantDozeRamp.SETTING, nowMs);
            } catch (Throwable t) {
                logFailure("could not report the instant doze brightness to SystemUI", t);
            }
        });
    }

    private static synchronized Executor stampWriter() {
        if (stampWriter == null) {
            stampWriter = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "LunaaAOD-doze-ramp");
                thread.setDaemon(true);
                return thread;
            });
        }
        return stampWriter;
    }

    private static synchronized void logFailure(String message, Throwable t) {
        if (failureLogged) return;
        failureLogged = true;
        Log.e(TAG, message, t);
        XposedBridge.log(TAG + ": " + message);
        XposedBridge.log(t);
    }
}
