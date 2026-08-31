package dev.lunaa.aod;

import static org.junit.Assert.*;

import org.junit.Test;

public class BrightnessSmootherTest {
    @Test public void brighteningUsesFourHundredMilliseconds() {
        BrightnessSmoother smoother = new BrightnessSmoother();
        smoother.snap(1000, 0.100f);
        smoother.retarget(1000, 0.500f);

        assertEquals(400L, BrightnessSmoother.BRIGHTEN_DURATION_MS);
        assertEquals(100L, BrightnessSmoother.FRAME_INTERVAL_MS);
        assertEquals(0.100f, smoother.valueAt(1000), 0.0001f);
        assertEquals(0.300f, smoother.valueAt(1200), 0.0001f);
        assertTrue(smoother.isRunning(1399));
        assertEquals(0.500f, smoother.valueAt(1400), 0.0001f);
        assertFalse(smoother.isRunning(1400));
    }

    @Test public void darkeningUsesFourteenHundredMilliseconds() {
        BrightnessSmoother smoother = new BrightnessSmoother();
        smoother.snap(1000, 0.500f);
        smoother.retarget(1000, 0.100f);

        assertEquals(1400L, BrightnessSmoother.DARKEN_DURATION_MS);
        assertEquals(0.300f, smoother.valueAt(1700), 0.0001f);
        assertTrue(smoother.isRunning(2399));
        assertEquals(0.100f, smoother.valueAt(2400), 0.0001f);
        assertFalse(smoother.isRunning(2400));
    }

    @Test public void retargetStartsFromCurrentInterpolatedValue() {
        BrightnessSmoother smoother = new BrightnessSmoother();
        smoother.snap(1000, 0.100f);
        smoother.retarget(1000, 0.500f);

        assertEquals(0.300f, smoother.valueAt(1200), 0.0001f);
        smoother.retarget(1200, 0.200f);

        assertEquals(0.300f, smoother.valueAt(1200), 0.0001f);
        assertEquals(0.250f, smoother.valueAt(1900), 0.0001f);
        assertEquals(0.200f, smoother.valueAt(2600), 0.0001f);
    }

    @Test public void firstTargetWithoutSeedSnapsImmediately() {
        BrightnessSmoother smoother = new BrightnessSmoother();
        assertTrue(Float.isNaN(smoother.valueAt(1000)));

        smoother.retarget(1000, 0.333f);

        assertEquals(0.333f, smoother.valueAt(1000), 0.0001f);
        assertFalse(smoother.isRunning(1000));
    }

    @Test public void targetsAreClampedToDisplayRequestRange() {
        BrightnessSmoother smoother = new BrightnessSmoother();
        smoother.snap(1000, -10f);
        assertEquals(AodSettingsSnapshot.MIN_BRIGHTNESS, smoother.valueAt(1000), 0.0001f);

        smoother.retarget(1000, 10f);
        assertEquals(AodSettingsSnapshot.MAX_BRIGHTNESS, smoother.target(), 0.0001f);
        assertEquals(AodSettingsSnapshot.MAX_BRIGHTNESS, smoother.valueAt(1400), 0.0001f);
    }
}
