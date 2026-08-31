package dev.lunaa.aod;

import android.content.Context;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class SystemUiHooks {
    private static final String TAG = "LunaaAOD";
    private static final String DOZE_BRIGHTNESS = "com.android.systemui.doze.DozeScreenBrightness";
    private static final String DOZE_STATE = "com.android.systemui.doze.DozeMachine$State";

    private static final Map<Object, AdaptiveAodController> CONTROLLERS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<String> ATTACH_FAILURES_LOGGED =
            Collections.synchronizedSet(new HashSet<>());
    private static boolean installAttempted;
    private static boolean transitionObservedLogged;

    private SystemUiHooks() {}

    public static synchronized void install(ClassLoader classLoader) {
        if (installAttempted) {
            Log.i(TAG, "hooks already attempted; skipping duplicate install");
            return;
        }
        installAttempted = true;

        Class<?> brightnessClass = XposedHelpers.findClass(DOZE_BRIGHTNESS, classLoader);
        Class<?> stateClass = XposedHelpers.findClass(DOZE_STATE, classLoader);

        XposedHelpers.findAndHookMethod(
                brightnessClass,
                "transitionTo",
                stateClass,
                stateClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!transitionObservedLogged) {
                            transitionObservedLogged = true;
                            XposedBridge.log(TAG + ": transitionTo observed");
                        }
                        AdaptiveAodController controller = ensureController(param.thisObject);
                        if (controller != null) {
                            Object newState = param.args != null && param.args.length > 1 ? param.args[1] : null;
                            controller.captureScreenBrightness();
                            controller.onDozeTransition(newState);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        AdaptiveAodController controller = CONTROLLERS.get(param.thisObject);
                        if (controller == null) return;
                        Object newState = param.args != null && param.args.length > 1 ? param.args[1] : null;
                        if (newState != null && "FINISH".equals(String.valueOf(newState))) {
                            controller.destroy();
                            CONTROLLERS.remove(param.thisObject);
                        } else {
                            controller.refreshDisplayState();
                        }
                    }
                }
        );

        try {
            XposedHelpers.findAndHookMethod(
                    brightnessClass,
                    "resetBrightnessToDefault",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            AdaptiveAodController controller = CONTROLLERS.get(param.thisObject);
                            if (controller != null) {
                                controller.reapplyAfterReset();
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            Log.w(TAG, "resetBrightnessToDefault hook unavailable; continuing without reset hook", t);
            XposedBridge.log(TAG + ": resetBrightnessToDefault hook unavailable; continuing without reset hook");
        }
    }

    private static AdaptiveAodController ensureController(Object instance) {
        if (instance == null) return null;
        AdaptiveAodController existing = CONTROLLERS.get(instance);
        if (existing != null) return existing;

        Object sensorValue = RuntimeFieldResolver.readExactOrUniqueAssignable(instance, "mSensorManager", SensorManager.class);
        if (!(sensorValue instanceof SensorManager)) {
            logAttachFailure("mSensorManager unavailable");
            return null;
        }

        Object displayValue = RuntimeFieldResolver.readExactOrUniqueAssignable(instance, "mDisplayManager", DisplayManager.class);
        if (!(displayValue instanceof DisplayManager)) {
            logAttachFailure("mDisplayManager unavailable");
            return null;
        }

        Object handlerValue = RuntimeFieldResolver.readExactOrUniqueAssignable(instance, "mHandler", Handler.class);
        if (!(handlerValue instanceof Handler)) {
            logAttachFailure("mHandler unavailable");
            return null;
        }

        Object contextValue = RuntimeFieldResolver.readExactOrUniqueAssignable(displayValue, "mContext", Context.class);
        Context systemUiContext = contextValue instanceof Context ? (Context) contextValue : null;
        if (systemUiContext == null && ATTACH_FAILURES_LOGGED.add("extraBright-context-unavailable")) {
            Log.w(TAG, "extraBright context unavailable; normal adaptive AOD remains active");
            XposedBridge.log(TAG + ": extraBright context unavailable; normal adaptive AOD remains active");
        }

        synchronized (CONTROLLERS) {
            existing = CONTROLLERS.get(instance);
            if (existing != null) return existing;
            try {
                AdaptiveAodController created = new AdaptiveAodController(
                        instance,
                        (SensorManager) sensorValue,
                        (DisplayManager) displayValue,
                        (Handler) handlerValue,
                        new XposedSettingsReader(),
                        systemUiContext
                );
                CONTROLLERS.put(instance, created);
                Log.i(TAG, "controller attached source=runtime-fields");
                XposedBridge.log(TAG + ": controller attached source=runtime-fields");
                return created;
            } catch (Throwable t) {
                logAttachFailure("controller initialization failed", t);
                return null;
            }
        }
    }


    private static void logAttachFailure(String reason) {
        logAttachFailure(reason, null);
    }

    private static void logAttachFailure(String reason, Throwable cause) {
        if (ATTACH_FAILURES_LOGGED.add(reason)) {
            if (cause == null) {
                Log.w(TAG, "controller attach failed: " + reason);
            } else {
                Log.e(TAG, "controller attach failed: " + reason, cause);
            }
            XposedBridge.log(TAG + ": controller attach failed: " + reason);
            if (cause != null) {
                XposedBridge.log(cause);
            }
        }
    }
}
