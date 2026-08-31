package dev.lunaa.aod;

public final class RootCommandGateTestMain {
    public static void main(String[] args) {
        RootCommandGate gate = new RootCommandGate();
        long enable1 = gate.beginCommand();
        yes(gate.isCurrent(enable1));
        long reset = gate.beginCommand();
        no(gate.isCurrent(enable1));
        yes(gate.isCurrent(reset));
        long enable2 = gate.beginCommand();
        no(gate.isCurrent(reset));
        yes(gate.isCurrent(enable2));
        System.out.println("PASS RootCommandGateTestMain");
    }

    private static void yes(boolean value) {
        if (!value) throw new AssertionError("expected current token");
    }

    private static void no(boolean value) {
        if (value) throw new AssertionError("expected superseded token");
    }
}
