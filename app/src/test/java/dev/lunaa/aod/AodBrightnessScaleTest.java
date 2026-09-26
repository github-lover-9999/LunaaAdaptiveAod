package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AodBrightnessScaleTest {
    @Test public void hbmTransitionPointIsTheBrightestAodBrightness() {
        // lunaa (crDroid display config): the normal range ends at backlight 2047 of 2784, and the
        // brightness slider reaches the whole range.
        assertEquals(0.73527f, AodBrightnessScale.fullScale(0.73527f, 1f), 0.0001f);
        assertEquals(0.73527f, AodBrightnessScale.fullScale(0.73527f, 0.73527f), 0.0001f);
    }

    @Test public void sliderMaximumMarksTheNormalRangeWhenTheRomHasNoHbmData() {
        assertEquals(0.73527f, AodBrightnessScale.fullScale(Float.POSITIVE_INFINITY, 0.73527f), 0.0001f);
    }

    @Test public void lowerOfTheTwoLimitsWins() {
        assertEquals(0.45f, AodBrightnessScale.fullScale(0.5f, 0.45f), 0.0001f);
        assertEquals(0.45f, AodBrightnessScale.fullScale(0.45f, 0.5f), 0.0001f);
    }

    @Test public void fullRangeWhenNothingLimitsIt() {
        assertEquals(1f, AodBrightnessScale.fullScale(Float.POSITIVE_INFINITY, 1f), 0f);
        assertEquals(1f, AodBrightnessScale.fullScale(Float.NaN, Float.NaN), 0f);
    }

    @Test public void implausibleValuesAreIgnored() {
        assertEquals(1f, AodBrightnessScale.fullScale(0f, -1f), 0f);
        assertEquals(1f, AodBrightnessScale.fullScale(0.02f, 1.5f), 0f);
    }
}
