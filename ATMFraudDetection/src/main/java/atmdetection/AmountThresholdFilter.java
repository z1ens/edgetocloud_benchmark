package atmdetection;

import org.apache.flink.api.common.functions.FilterFunction;

public class AmountThresholdFilter implements FilterFunction<ATMTransaction> {
    private final double threshold;

    public AmountThresholdFilter(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean filter(ATMTransaction txn) {
        return txn.getAmount() >= threshold;
    }
}
