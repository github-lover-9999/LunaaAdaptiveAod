package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExtraBrightnessLevelTest {
    @Test public void defaultsToLevelTwoSeventyFivePercent() {
        assertEquals(75, ExtraBrightnessLevel.DEFAULT_PERCENT);
        assertEquals(0.25f, ExtraBrightnessLevel.overlayAlphaForPercent(75), 0.0001f);
    }

    @Test public void supportsFullOneToHundredPercentAdvancedRange() {
        assertEquals(1, ExtraBrightnessLevel.normalize(0));
        assertEquals(100, ExtraBrightnessLevel.normalize(120));
        assertEquals(0.50f, ExtraBrightnessLevel.overlayAlphaForPercent(50), 0.0001f);
        assertEquals(0.25f, ExtraBrightnessLevel.overlayAlphaForPercent(75), 0.0001f);
        assertEquals(0.00f, ExtraBrightnessLevel.overlayAlphaForPercent(100), 0.0001f);
    }
}
