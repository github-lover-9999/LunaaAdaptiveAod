package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemServerHooksWiringTest {
    @Test public void moduleRoutesTheSystemFrameworkAndSystemUiSeparately() throws Exception {
        String module = read("LunaaAodModule.java");

        assertTrue(module.contains("SYSTEM_FRAMEWORK.equals(lpparam.packageName)"));
        assertTrue(module.contains("SYSTEM_FRAMEWORK.equals(lpparam.processName)"));
        assertTrue(module.contains("SystemServerHooks.install(lpparam.classLoader)"));
        assertTrue(module.contains("SystemUiHooks.install(lpparam.classLoader)"));
        assertTrue("a failed framework hook must keep the stock system running",
                module.contains("stock display ramp and Battery Saver retained"));
    }

    @Test public void onlyTheDozeRampOfDisplayPowerControllerIsChanged() throws Exception {
        String hooks = read("SystemServerHooks.java");

        assertTrue(hooks.contains("\"com.android.server.display.DisplayPowerController\""));
        assertTrue(hooks.contains("\"animateScreenBrightness\""));
        assertTrue(hooks.contains("float.class, float.class, float.class, boolean.class"));
        assertTrue(hooks.contains("\"mPowerRequest\""));
        assertTrue(hooks.contains("\"policy\""));
        assertTrue(hooks.contains("InstantDozeRamp.rampRate(policy, rate, enabled)"));
        assertTrue(hooks.contains("param.args[2] = "));
        assertFalse("never replaces the method", hooks.contains("setResult"));
        assertFalse(hooks.contains("afterHookedMethod"));
    }

    @Test public void hookFailsSafeAndHonoursTheModuleSwitch() throws Exception {
        String hooks = read("SystemServerHooks.java");

        assertTrue(hooks.contains("catch (Throwable t)"));
        assertTrue(hooks.contains("new XposedSettingsReader()"));
        assertTrue(hooks.contains(".isEnabled()"));
    }

    @Test public void systemUiLearnsAboutTheJumpFromAStampWrittenOffTheDisplayThread() throws Exception {
        String hooks = read("SystemServerHooks.java");
        String probe = read("InstantRampProbe.java");

        assertTrue(hooks.contains("Settings.Global.putLong("));
        assertTrue(hooks.contains("InstantDozeRamp.SETTING"));
        assertTrue(hooks.contains("Executors.newSingleThreadExecutor("));
        assertTrue(probe.contains("Settings.Global.getLong("));
        assertTrue(probe.contains("InstantDozeRamp.confirmedSince("));
    }

    @Test public void manifestScopesSystemUiAndTheSystemFramework() throws Exception {
        String manifest = TestProjectFiles.read("app/src/main/AndroidManifest.xml");
        String arrays = TestProjectFiles.read("app/src/main/res/values/arrays.xml");

        assertTrue(manifest.contains("android:name=\"xposedscope\" android:resource=\"@array/xposed_scope\""));
        assertTrue(arrays.contains("<string-array name=\"xposed_scope\">"));
        assertTrue(arrays.contains("<item>android</item>"));
        assertTrue(arrays.contains("<item>com.android.systemui</item>"));
    }

    private static String read(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
