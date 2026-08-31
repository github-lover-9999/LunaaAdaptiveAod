package de.robv.android.xposed;
public abstract class XC_MethodHook {
    public class Unhook {}
    public static class MethodHookParam {
        public Object thisObject;
        public Object[] args;
    }
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
}
