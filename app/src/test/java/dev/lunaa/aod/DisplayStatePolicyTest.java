package dev.lunaa.aod;
import static org.junit.Assert.*;
import org.junit.Test;
public class DisplayStatePolicyTest {
    @Test public void onlyDozeStatesAreAmbient() {
        assertTrue(DisplayStatePolicy.isAmbientState(3));
        assertTrue(DisplayStatePolicy.isAmbientState(4));
        assertFalse(DisplayStatePolicy.isAmbientState(1));
        assertFalse(DisplayStatePolicy.isAmbientState(2));
    }

    @Test public void offAndDozeStatesAreLowPowerButOnAndUnknownAreNot() {
        assertTrue(DisplayStatePolicy.isLowPowerState(1));
        assertTrue(DisplayStatePolicy.isLowPowerState(3));
        assertTrue(DisplayStatePolicy.isLowPowerState(4));
        assertFalse(DisplayStatePolicy.isLowPowerState(2));
        assertFalse(DisplayStatePolicy.isLowPowerState(0));
    }
}
