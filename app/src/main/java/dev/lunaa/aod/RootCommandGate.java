package dev.lunaa.aod;

/** Latest-command-wins generation gate for root HBM/reset operations. */
final class RootCommandGate {
    private static final int UNKNOWN = -1;

    private long generation;
    /** What the module's last root write left in notify_fppress, as far as this process knows. */
    private int lastWrite = UNKNOWN;

    synchronized long beginCommand() {
        generation++;
        if (generation == Long.MIN_VALUE) generation = 1L;
        return generation;
    }

    synchronized boolean isCurrent(long token) {
        return token == generation;
    }

    synchronized void recordWrite(String value, boolean ok) {
        lastWrite = ok ? ("1".equals(value) ? 1 : 0) : UNKNOWN;
    }

    /**
     * A reset only releases the module's own synthetic press. Once the module's last write
     * released it (for example an edge a reset superseded before its 1), writing 0 again could
     * only cancel a real finger press the stock fingerprint path made since.
     */
    synchronized boolean resetNeedsWrite() {
        return lastWrite != 0;
    }
}
