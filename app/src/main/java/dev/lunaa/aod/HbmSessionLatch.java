package dev.lunaa.aod;

/**
 * Tracks the module's assumed physical HBM latch separately from the synthetic
 * logical fingerprint reset used to leave the real UDFPS path ready for a user touch.
 */
final class HbmSessionLatch {
    private boolean latched;
    private boolean logicalRearmActive;

    void markLatched() {
        latched = true;
    }

    boolean isLatched() {
        return latched;
    }

    void beginLogicalRearm() {
        logicalRearmActive = true;
    }

    void endLogicalRearm() {
        logicalRearmActive = false;
    }

    boolean isLogicalRearmActive() {
        return logicalRearmActive;
    }

    /**
     * @return true when a real/stock reset invalidated a previously assumed HBM latch.
     */
    boolean onStockReset() {
        if (logicalRearmActive) return false;
        boolean wasLatched = latched;
        latched = false;
        return wasLatched;
    }

    void clear() {
        latched = false;
        logicalRearmActive = false;
    }
}
