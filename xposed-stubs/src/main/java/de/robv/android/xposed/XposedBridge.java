package de.robv.android.xposed;
import java.util.Set;
public final class XposedBridge {
    private XposedBridge() {}
    public static void log(String text) {}
    public static void log(Throwable throwable) {}
    public static Set<?> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) { return null; }
}
