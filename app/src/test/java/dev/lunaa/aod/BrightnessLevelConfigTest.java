package dev.lunaa.aod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class BrightnessLevelConfigTest {
    @Test public void selectedLevelReturnsMappedPercent() {
        BrightnessLevelConfig c = new BrightnessLevelConfig(2, 10, 30, 100);
        assertEquals(2, c.getSelectedLevel());
        assertEquals(10, c.getPercent(1));
        assertEquals(30, c.getSelectedPercent());
        assertEquals(100, c.getPercent(3));
    }

    @Test public void advancedValuesMustBeOneToHundredAndNondecreasing() {
        expectIllegal(() -> new BrightnessLevelConfig(1, 0, 30, 100));
        expectIllegal(() -> new BrightnessLevelConfig(1, 10, 101, 100));
        expectIllegal(() -> new BrightnessLevelConfig(1, 50, 40, 100));
        expectIllegal(() -> new BrightnessLevelConfig(4, 10, 30, 100));
    }

    @Test public void closestLevelSupportsLegacyMigration() {
        assertEquals(1, BrightnessLevelConfig.closestLevel(12, 10, 30, 100));
        assertEquals(2, BrightnessLevelConfig.closestLevel(86, 50, 75, 100));
        assertEquals(3, BrightnessLevelConfig.closestLevel(99, 10, 30, 100));
    }

    private static void expectIllegal(Runnable r) {
        try { r.run(); fail("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
}
