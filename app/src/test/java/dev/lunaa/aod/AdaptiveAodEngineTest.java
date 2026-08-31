package dev.lunaa.aod;

import static org.junit.Assert.*;

import org.junit.Test;

public class AdaptiveAodEngineTest {
    @Test public void screenBrightnessSeedsThenLuxTakesOver() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        engine.captureScreenBrightness(0.147f);
        assertEquals(BrightnessCurve.initialFromScreenBrightness(0.147f), engine.setAmbientActive(true, 1000), 0.0002f);
        assertEquals(BrightnessCurve.targetForLux(487f), engine.onLux(1100, 487f), 0.001f);
        assertTrue(Float.isNaN(engine.onLux(1150, 488f)));
        assertEquals(engine.currentTarget(), engine.reapply(), 0.0001f);
        engine.setAmbientActive(false, 1200);
        assertTrue(Float.isNaN(engine.reapply()));
        assertEquals(BrightnessCurve.targetForLux(487f), engine.setAmbientActive(true, 5000), 0.001f);
    }

    @Test public void remembersLuxObservedBeforeEnteringDoze() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        assertTrue(Float.isNaN(engine.onLux(1000, 487f)));
        assertEquals(BrightnessCurve.targetForLux(487f), engine.setAmbientActive(true, 1500), 0.001f);
    }

    @Test public void preparesPendingTargetBeforePhysicalDoze() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        assertTrue(Float.isNaN(engine.onLux(1000, 500f)));

        assertEquals(BrightnessCurve.targetForLux(500f), engine.prepareAmbientEntry(1500), 0.0001f);
        assertEquals(BrightnessCurve.targetForLux(500f), engine.pendingTarget(), 0.0001f);
        assertFalse(engine.isAmbientActive());
        assertTrue(Float.isNaN(engine.reapply()));

        engine.clearPendingAmbientEntry();
        assertTrue(Float.isNaN(engine.pendingTarget()));
    }

    @Test public void manualModeAlwaysUsesFixedBrightnessAndStillRemembersLux() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        engine.updateSettings(configured(base, true, AodMode.MANUAL, 250, 0.090f, 0.333f));

        assertTrue(Float.isNaN(engine.onLux(1000, 500f)));
        assertEquals(0.333f, engine.prepareAmbientEntry(1200), 0.0001f);
        assertEquals(0.333f, engine.setAmbientActive(true, 1300), 0.0001f);
        assertTrue(Float.isNaN(engine.onLux(1400, 20000f)));
        assertEquals(0.333f, engine.currentTarget(), 0.0001f);
        assertEquals(0.333f, engine.reapply(), 0.0001f);
    }

    @Test public void firstLargeDarkeningSampleIsGuardedButSecondConsistentSampleApplies() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        assertTrue(Float.isNaN(engine.onLux(1000, 500f)));
        assertEquals(BrightnessCurve.targetForLux(500f), engine.prepareAmbientEntry(1500), 0.0001f);
        assertEquals(BrightnessCurve.targetForLux(500f), engine.setAmbientActive(true, 1520), 0.0001f);

        assertTrue(Float.isNaN(engine.onLux(1600, 0f)));
        assertEquals(BrightnessCurve.targetForLux(0f), engine.onLux(1700, 0f), 0.0001f);
        assertEquals(BrightnessCurve.targetForLux(0f), engine.currentTarget(), 0.0001f);
    }

    @Test public void entryGuardNeverDelaysBrightening() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        assertTrue(Float.isNaN(engine.onLux(1000, 10f)));
        assertEquals(BrightnessCurve.targetForLux(10f), engine.setAmbientActive(true, 1500), 0.0001f);

        assertEquals(BrightnessCurve.targetForLux(500f), engine.onLux(1550, 500f), 0.0001f);
    }

    @Test public void singleDarkeningSampleAppliesAfterEntryGuardExpires() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        assertTrue(Float.isNaN(engine.onLux(1000, 500f)));
        assertEquals(BrightnessCurve.targetForLux(500f), engine.setAmbientActive(true, 1500), 0.0001f);

        assertEquals(BrightnessCurve.targetForLux(0f), engine.onLux(2750, 0f), 0.0001f);
    }

    @Test public void legacyAutomaticMinimumDoesNotOverridePresetCurve() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        engine.updateSettings(configured(base, true, AodMode.AUTOMATIC, 50, 0.100f, 0.150f));

        assertTrue(Float.isNaN(engine.onLux(1000, 0f)));
        assertEquals(BrightnessCurve.targetForLux(0f), engine.setAmbientActive(true, 1500), 0.0001f);
    }

    @Test public void disabledSettingsRememberLuxButNeverApply() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();
        engine.updateSettings(configured(base, false, AodMode.AUTOMATIC, 100, 0.020f, 0.150f));

        assertTrue(Float.isNaN(engine.onLux(1000, 500f)));
        assertTrue(Float.isNaN(engine.prepareAmbientEntry(1400)));
        assertTrue(Float.isNaN(engine.setAmbientActive(true, 1500)));
        assertTrue(Float.isNaN(engine.onLux(1600, 1000f)));
        assertTrue(Float.isNaN(engine.reapply()));
    }

    @Test public void luxObservationPolicyFollowsEnabledAutomaticMode() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        AodSettingsSnapshot base = AodSettingsDefaults.balanced();

        engine.updateSettings(configured(base, true, AodMode.AUTOMATIC, 100, 0.020f, 0.150f));
        assertTrue(engine.isEnabled());
        assertTrue(engine.isAutomaticMode());
        assertTrue(engine.shouldObserveLux(true, false));
        assertTrue(engine.shouldObserveLux(false, true));
        assertFalse(engine.shouldObserveLux(false, false));

        engine.updateSettings(configured(base, true, AodMode.MANUAL, 100, 0.020f, 0.250f));
        assertTrue(engine.isEnabled());
        assertFalse(engine.isAutomaticMode());
        assertFalse(engine.shouldObserveLux(true, false));
        assertFalse(engine.shouldObserveLux(false, true));

        engine.updateSettings(configured(base, false, AodMode.AUTOMATIC, 100, 0.020f, 0.150f));
        assertFalse(engine.isEnabled());
        assertFalse(engine.shouldObserveLux(true, false));
        assertFalse(engine.shouldObserveLux(false, true));
    }

    private static AodSettingsSnapshot configured(
            AodSettingsSnapshot base,
            boolean enabled,
            AodMode mode,
            int multiplier,
            float minimum,
            float manual
    ) {
        return new AodSettingsSnapshot(
                enabled,
                mode,
                multiplier,
                minimum,
                manual,
                base.copyLux(),
                base.copyBrightness(),
                3
        );
    }
    @Test
    public void nullSettingsFailClosedToStockBehavior() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        engine.updateSettings(null);

        assertFalse(engine.isEnabled());
        assertTrue(Float.isNaN(engine.prepareAmbientEntry(1_000L)));
    }

}
