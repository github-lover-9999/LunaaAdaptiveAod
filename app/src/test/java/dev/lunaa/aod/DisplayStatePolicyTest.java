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
}
