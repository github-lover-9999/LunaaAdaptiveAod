package dev.lunaa.aod;

/**
 * Tracks the module's assumed physical HBM latch separately from the synthetic
 * logical fingerprint reset used to leave the real UDFPS path ready for a user touch.
 */
final class HbmSessionLatch {
    private boolean latched;
    private boolean logicalRearmActive;
    private boolean logicalPressReleased;
    private boolean yieldedToStockFingerprint;

    void markLatched() {
        latched = true;
        logicalPressReleased = false;
    }

    boolean isLatched() {
        return latched;
    }

    /** Our own logical reset reached the kernel; the physical HBM latch is kept. */
    void markLogicalPressReleased() {
        logicalPressReleased = true;
    }

    /** The module's synthetic 0->1 press may still be the kernel's logical notify_fppress state. */
    boolean isLogicalPressHeld() {
        return latched && !logicalPressReleased;
    }

    /** SystemUI took the panel out of AOD: the stock UDFPS path owns notify_fppress. */
    void yieldToStockFingerprint() {
        yieldedToStockFingerprint = true;
    }

    void resumeFromStockFingerprint() {
        yieldedToStockFingerprint = false;
    }

    boolean isYieldedToStockFingerprint() {
        return yieldedToStockFingerprint;
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
        logicalPressReleased = false;
        yieldedToStockFingerprint = false;
    }
}
