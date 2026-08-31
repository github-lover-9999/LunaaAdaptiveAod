package dev.lunaa.aod;

/** Immutable three-level percentage mapping used by Manual and Extra Bright controls. */
public final class BrightnessLevelConfig {
    public static final int LEVEL_COUNT = 3;
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 3;
    public static final int MIN_PERCENT = 1;
    public static final int MAX_PERCENT = 100;

    private final int selectedLevel;
    private final int[] percents;

    public BrightnessLevelConfig(int selectedLevel, int level1Percent, int level2Percent, int level3Percent) {
        validateLevel(selectedLevel);
        validatePercent(level1Percent, "level1Percent");
        validatePercent(level2Percent, "level2Percent");
        validatePercent(level3Percent, "level3Percent");
        if (level1Percent > level2Percent || level2Percent > level3Percent) {
            throw new IllegalArgumentException("brightness levels must be nondecreasing");
        }
        this.selectedLevel = selectedLevel;
        this.percents = new int[]{level1Percent, level2Percent, level3Percent};
    }

    public int getSelectedLevel() {
        return selectedLevel;
    }

    public int getPercent(int level) {
        validateLevel(level);
        return percents[level - 1];
    }

    public int getSelectedPercent() {
        return getPercent(selectedLevel);
    }

    public static int closestLevel(int percent, int level1Percent, int level2Percent, int level3Percent) {
        int safe = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
        int[] values = {level1Percent, level2Percent, level3Percent};
        int bestLevel = 1;
        int bestDistance = Math.abs(safe - values[0]);
        for (int i = 1; i < values.length; i++) {
            int distance = Math.abs(safe - values[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestLevel = i + 1;
            }
        }
        return bestLevel;
    }

    private static void validateLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("level must be 1..3");
        }
    }

    private static void validatePercent(int percent, String name) {
        if (percent < MIN_PERCENT || percent > MAX_PERCENT) {
            throw new IllegalArgumentException(name + " must be 1..100");
        }
    }
}
