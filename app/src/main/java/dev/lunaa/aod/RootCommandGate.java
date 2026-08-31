package dev.lunaa.aod;

/** Latest-command-wins generation gate for root HBM/reset operations. */
final class RootCommandGate {
    private long generation;

    synchronized long beginCommand() {
        generation++;
        if (generation == Long.MIN_VALUE) generation = 1L;
        return generation;
    }

    synchronized boolean isCurrent(long token) {
        return token == generation;
    }
}
