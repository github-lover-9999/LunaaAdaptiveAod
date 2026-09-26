package de.robv.android.xposed;
public abstract class XC_MethodHook {
    public class Unhook {}
    public static class MethodHookParam {
        public Object thisObject;
        public Object[] args;
        private Object result;
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
    }
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
}
