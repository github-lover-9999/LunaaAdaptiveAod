package de.robv.android.xposed;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Set;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static org.junit.Assert.assertEquals;

public class XposedStubAbiContractTest {
    @Test
    public void xposedHelpersMethodsMatchLegacyAbi() throws Exception {
        Method findClass = XposedHelpers.class.getDeclaredMethod(
                "findClass", String.class, ClassLoader.class);
        assertEquals(Class.class, findClass.getReturnType());

        Method findAndHook = XposedHelpers.class.getDeclaredMethod(
                "findAndHookMethod", Class.class, String.class, Object[].class);
        assertEquals(XC_MethodHook.Unhook.class, findAndHook.getReturnType());

        Method getObjectField = XposedHelpers.class.getDeclaredMethod(
                "getObjectField", Object.class, String.class);
        assertEquals(Object.class, getObjectField.getReturnType());

        Method callMethod = XposedHelpers.class.getDeclaredMethod(
                "callMethod", Object.class, String.class, Object[].class);
        assertEquals(Object.class, callMethod.getReturnType());
    }

    @Test
    public void xposedBridgeMethodsMatchLegacyAbi() throws Exception {
        Method hookAllConstructors = XposedBridge.class.getDeclaredMethod(
                "hookAllConstructors", Class.class, XC_MethodHook.class);
        assertEquals(Set.class, hookAllConstructors.getReturnType());

        Method logString = XposedBridge.class.getDeclaredMethod("log", String.class);
        assertEquals(void.class, logString.getReturnType());

        Method logThrowable = XposedBridge.class.getDeclaredMethod("log", Throwable.class);
        assertEquals(void.class, logThrowable.getReturnType());
    }

    @Test
    public void xSharedPreferencesMethodsMatchLegacyAbi() throws Exception {
        XSharedPreferences.class.getDeclaredConstructor(String.class, String.class);
        assertEquals(void.class, XSharedPreferences.class.getDeclaredMethod("reload").getReturnType());
        assertEquals(boolean.class, XSharedPreferences.class.getDeclaredMethod(
                "getBoolean", String.class, boolean.class).getReturnType());
        assertEquals(int.class, XSharedPreferences.class.getDeclaredMethod(
                "getInt", String.class, int.class).getReturnType());
        assertEquals(float.class, XSharedPreferences.class.getDeclaredMethod(
                "getFloat", String.class, float.class).getReturnType());
    }

    @Test
    public void loadPackageCallbackMatchesLegacyAbi() throws Exception {
        Method callback = IXposedHookLoadPackage.class.getDeclaredMethod(
                "handleLoadPackage", XC_LoadPackage.LoadPackageParam.class);
        assertEquals(void.class, callback.getReturnType());
    }
}
