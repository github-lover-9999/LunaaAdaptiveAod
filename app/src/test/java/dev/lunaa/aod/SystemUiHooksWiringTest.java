package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemUiHooksWiringTest {
    @Test
    public void transitionIsTheOnlyControllerCreationPath() throws Exception {
        String hooks = readSource("SystemUiHooks.java");

        assertFalse("v1.1.0 must not hook constructors",
                hooks.contains("hookAllConstructors"));
        assertTrue("v1.5.4 must resolve Context from DisplayManager for the app-root bridge",
                hooks.contains("RuntimeFieldResolver.readExactOrUniqueAssignable(displayValue, \"mContext\", Context.class)"));
        assertTrue("missing Context must disable only Extra Bright, not the normal controller",
                hooks.contains("extraBright context unavailable; normal adaptive AOD remains active"));
        assertFalse("v1.1.0 must not scan constructor args for Context",
                hooks.contains("firstContext"));
        assertFalse("v1.1.0 must not use ActivityThread fallback",
                hooks.contains("ActivityThread"));

        int transitionStart = hooks.indexOf("\"transitionTo\"");
        int resetStart = hooks.indexOf("\"resetBrightnessToDefault\"");
        assertTrue("transition hook must exist", transitionStart >= 0);
        assertTrue("reset hook must exist after transition hook", resetStart > transitionStart);

        int ensureControllerDefinition = hooks.indexOf("private static AdaptiveAodController ensureController", resetStart);
        assertTrue("ensureController definition must follow reset hook", ensureControllerDefinition > resetStart);

        String transitionHook = hooks.substring(transitionStart, resetStart);
        String resetHook = hooks.substring(resetStart, ensureControllerDefinition);
        assertTrue("transition hook must create/obtain the controller",
                transitionHook.contains("ensureController(param.thisObject)"));
        assertFalse("reset hook must never create a controller",
                resetHook.contains("ensureController("));
        assertTrue("reset hook must only reuse an existing controller",
                resetHook.contains("CONTROLLERS.get(param.thisObject)"));
        assertTrue("reset hook installation must be optional for ROM variants",
                hooks.contains("resetBrightnessToDefault hook unavailable; continuing without reset hook"));
    }

    @Test
    public void runtimeDependenciesAreReadAndTypeChecked() throws Exception {
        String hooks = readSource("SystemUiHooks.java");

        assertTrue(hooks.contains("RuntimeFieldResolver.readExactOrUniqueAssignable(instance, \"mSensorManager\", SensorManager.class)"));
        assertTrue(hooks.contains("RuntimeFieldResolver.readExactOrUniqueAssignable(instance, \"mDisplayManager\", DisplayManager.class)"));
        assertTrue(hooks.contains("RuntimeFieldResolver.readExactOrUniqueAssignable(instance, \"mHandler\", Handler.class)"));
        assertTrue(hooks.contains("instanceof SensorManager"));
        assertTrue(hooks.contains("instanceof DisplayManager"));
        assertTrue(hooks.contains("instanceof Handler"));
        assertTrue(hooks.contains("controller attached source=runtime-fields"));
        assertTrue(hooks.contains("controller attach failed:"));
    }

    @Test
    public void controllerConsumesResolvedSystemUiDependenciesDirectly() throws Exception {
        String controller = readSource("AdaptiveAodController.java");

        assertTrue("controller must receive SystemUI Context only for the root bridge",
                controller.contains("android.content.Context"));
        assertFalse("controller must not create a new main-thread Handler", controller.contains("new Handler("));
        assertFalse("controller must not use Looper", controller.contains("Looper"));
        assertFalse("controller must not call getSystemService", controller.contains("getSystemService"));
        assertTrue(controller.contains(
                "AdaptiveAodController(Object dozeScreenBrightness, SensorManager sensorManager, DisplayManager displayManager, Handler handler, XposedSettingsReader settingsReader, Context systemUiContext)"));
        assertTrue(controller.contains("this.sensorManager = sensorManager"));
        assertTrue(controller.contains("this.displayManager = displayManager"));
        assertTrue(controller.contains("this.handler = handler"));
    }

    @Test
    public void lifecycleEvidenceRemainsVectorVisible() throws Exception {
        String hooks = readSource("SystemUiHooks.java");
        assertTrue(hooks.contains("transitionTo observed"));
        assertTrue(hooks.contains("XposedBridge.log(TAG + \": controller attached source=runtime-fields\")"));
    }

    private static String readSource(String fileName) throws Exception {
        return TestProjectFiles.read("app/src/main/java/dev/lunaa/aod/" + fileName);
    }
}
