package atmdetection;

public class FraudHeuristic {
    // Simplified fraud detection rule
    public static boolean isSuspicious(double amount, int countInWindow, double totalInWindow) {
        return amount > 1000 && countInWindow >= 3 && totalInWindow > 5000;
    }

    // Extend here to simulate other ML-like logic (e.g., time of day, ATM ID, etc.)
}
