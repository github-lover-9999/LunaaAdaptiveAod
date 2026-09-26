package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BatterySaverAodWiringTest {
    @Test public void frameworkHooksInstallIndependently() throws Exception {
        String hooks = read("SystemServerHooks.java");

        int ramp = hooks.indexOf("installDozeRampHook(classLoader);");
        int saver = hooks.indexOf("BatterySaverAodHook.install(classLoader);");
        assertTrue("both framework hooks are installed", ramp >= 0 && saver >= 0);
        String between = hooks.substring(Math.min(ramp, saver), Math.max(ramp, saver));
        assertTrue("a failure of one keeps the other", between.contains("catch (Throwable t)"));
    }

    @Test public void onlyTheAodAnswerOfBatterySaverPolicyChanges() throws Exception {
        String hook = read("BatterySaverAodHook.java");

        assertTrue(hook.contains("\"com.android.server.power.batterysaver.BatterySaverPolicy\""));
        assertTrue(hook.contains("\"getBatterySaverPolicy\""));
        assertTrue(hook.contains("int.class"));
        assertTrue(hook.contains("afterHookedMethod"));
        assertFalse("the policy itself runs unchanged", hook.contains("beforeHookedMethod"));
        assertTrue(hook.contains("BatterySaverAod.overrides("));
        assertTrue(hook.contains("param.setResult("));
        assertTrue(hook.contains("catch (Throwable t)"));
    }

    @Test public void theNewAnswerKeepsEveryOtherBatterySaverValue() throws Exception {
        String hook = read("BatterySaverAodHook.java");

        assertTrue(hook.contains("\"setBatterySaverEnabled\""));
        assertTrue(hook.contains("\"globalBatterySaverEnabled\""));
        assertTrue(hook.contains("\"locationMode\""));
        assertTrue(hook.contains("\"soundTriggerMode\""));
        assertTrue(hook.contains("\"brightnessFactor\""));
    }

    @Test public void theOptionIsReadFromTheModuleSettings() throws Exception {
        String hook = read("BatterySaverAodHook.java");
        String reader = read("XposedSettingsReader.java");
        String store = read("AndroidSettingsStore.java");
        String codec = read("AodSettingsCodec.java");

        assertTrue(codec.contains("KEY_KEEP_AOD_IN_BATTERY_SAVER = \"keep_aod_in_battery_saver\""));
        assertTrue(reader.contains("public boolean isAodKeptInBatterySaver()"));
        assertTrue(reader.contains("AodSettingsCodec.KEY_KEEP_AOD_IN_BATTERY_SAVER"));
        assertTrue(store.contains("public boolean loadAodKeptInBatterySaver()"));
        assertTrue(store.contains("public boolean saveAodKeptInBatterySaver(boolean keep)"));
        assertTrue(hook.contains("isAodKeptInBatterySaver()"));
    }

    @Test public void settingsScreenHasTheOptionAndSavesIt() throws Exception {
        String activity = read("SettingsActivity.java");
        String strings = TestProjectFiles.read("app/src/main/res/values/strings.xml");

        assertTrue(activity.contains("buildBatterySaverCard()"));
        assertTrue(activity.contains("batterySaverAodSwitch.setText(R.string.battery_saver_keep_aod)"));
        assertTrue(activity.contains("batterySaverAodSwitch.setChecked(settingsStore.loadAodKeptInBatterySaver())"));
        assertTrue(activity.contains("settingsStore.saveAodKeptInBatterySaver("));
        assertTrue("Save notices the change",
                activity.contains(".append(batterySaverAodSwitch != null && batterySaverAodSwitch.isChecked())"));
        assertTrue("Reset turns it off", activity.contains("batterySaverAodSwitch.setChecked(false);"));
        assertTrue(strings.contains("name=\"battery_saver_title\""));
        assertTrue(strings.contains("name=\"battery_saver_keep_aod\""));
        assertTrue(strings.contains("name=\"battery_saver_help\""));
    }

    private static String read(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
