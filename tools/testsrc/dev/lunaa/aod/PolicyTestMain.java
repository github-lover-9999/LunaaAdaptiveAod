package dev.lunaa.aod;

public final class PolicyTestMain {
    private static int assertions;

    public static void main(String[] args) {
        curveKnotsAndInterpolation();
        curveFallbackAndCap();
        automaticPresetSeparation();
        automaticExtraBrightThresholds();
        displayStateClassification();
        updateGateThresholds();
        engineLifecycle();
        preDozeLuxIsRemembered();
        System.out.println("PASS " + assertions + " assertions");
    }

    private static void curveKnotsAndInterpolation() {
        eq(0.0800f, BrightnessCurve.targetForLux(0f), 0.0001f);
        eq(0.0800f, BrightnessCurve.targetForLux(2f), 0.0001f);
        eq(0.1176f, BrightnessCurve.targetForLux(10f), 0.0001f);
        eq(0.0988f, BrightnessCurve.targetForLux(6f), 0.0001f);
        eq(0.2915f, BrightnessCurve.targetForLux(350f), 0.0001f);
    }

    private static void curveFallbackAndCap() {
        eq(0.0800f, BrightnessCurve.targetForLux(Float.NaN), 0.0001f);
        eq(0.0800f, BrightnessCurve.targetForLux(-1f), 0.0001f);
        eq(0.5500f, BrightnessCurve.targetForLux(100000f), 0.0001f);
        eq(0.2915f, BrightnessCurve.initialFromScreenBrightness(0.9f), 0.0001f);
        eq(0.0941f, BrightnessCurve.initialFromScreenBrightness(0.05f), 0.0001f);
        eq(0.0800f, BrightnessCurve.initialFromScreenBrightness(0.0f), 0.0001f);
    }

    private static void automaticPresetSeparation() {
        AodSettingsSnapshot dim = AodSettingsDefaults.dim();
        AodSettingsSnapshot balanced = AodSettingsDefaults.balanced();
        AodSettingsSnapshot bright = AodSettingsDefaults.bright();
        for (float lux : new float[]{50f, 500f, 1000f, 5000f}) {
            float d = BrightnessCurve.targetForLux(lux, dim);
            float b = BrightnessCurve.targetForLux(lux, balanced);
            float h = BrightnessCurve.targetForLux(lux, bright);
            yes(b - d >= 0.08f);
            yes(h - b >= 0.12f);
        }
    }

    private static void automaticExtraBrightThresholds() {
        eq(1500f, ExtraBrightnessPolicy.ENABLE_LUX, 0f);
        eq(700f, ExtraBrightnessPolicy.DISABLE_LUX, 0f);
        yes(ExtraBrightnessPolicy.ENABLE_DWELL_MS == 600L);
        yes(ExtraBrightnessPolicy.DISABLE_DWELL_MS == 1500L);
    }

    private static void displayStateClassification() {
        yes(DisplayStatePolicy.isAmbientState(3));
        yes(DisplayStatePolicy.isAmbientState(4));
        no(DisplayStatePolicy.isAmbientState(1));
        no(DisplayStatePolicy.isAmbientState(2));
    }

    private static void updateGateThresholds() {
        UpdateGate gate = new UpdateGate();
        yes(gate.shouldApply(1000, 100f, 0.08f));
        no(gate.shouldApply(1100, 105f, 0.081f));
        yes(gate.shouldApply(1200, 120f, 0.09f));
        no(gate.shouldApply(1300, 121f, 0.091f));
        yes(gate.shouldApply(3400, 128f, 0.10f));
    }

    private static void engineLifecycle() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        engine.captureScreenBrightness(0.147f);
        eq(0.121454f, engine.setAmbientActive(true, 1000), 0.0002f);
        eq(0.321549f, engine.onLux(1100, 487f), 0.001f);
        yes(Float.isNaN(engine.onLux(1150, 488f)));
        eq(engine.currentTarget(), engine.reapply(), 0.0001f);
        engine.setAmbientActive(false, 1200);
        yes(Float.isNaN(engine.reapply()));
        // A quick transient (e.g. UDFPS/display mode change) reuses fresh lux.
        eq(0.321549f, engine.setAmbientActive(true, 5000), 0.001f);
        engine.setAmbientActive(false, 5100);
        // After lux is stale, the latest normal screen brightness seeds the next session.
        engine.captureScreenBrightness(0.10f);
        eq(0.1082f, engine.setAmbientActive(true, 20000), 0.0002f);
    }

    private static void preDozeLuxIsRemembered() {
        AdaptiveAodEngine engine = new AdaptiveAodEngine();
        yes(Float.isNaN(engine.onLux(1000, 487f)));
        eq(0.321549f, engine.setAmbientActive(true, 1500), 0.001f);
    }

    private static void eq(float expected, float actual, float epsilon) {
        assertions++;
        if (Math.abs(expected - actual) > epsilon) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void yes(boolean value) {
        assertions++;
        if (!value) throw new AssertionError("expected true");
    }

    private static void no(boolean value) {
        assertions++;
        if (value) throw new AssertionError("expected false");
    }
}
