package atmdetection;

import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class SuspiciousTransactionDetector implements WindowFunction<ATMTransaction, String, String, TimeWindow> {
    @Override
    // current logic of alert triggering: if there are more than 3 big amount of transfer attempts -> trigger alert
    public void apply(String key, TimeWindow timeWindow, Iterable<ATMTransaction> input, Collector<String> out) throws Exception {
        int count = 0;
        double total = 0.0;
        for (ATMTransaction txn : input) {
            if (txn.getAmount() > 1000) {
                count++;
                total += txn.getAmount();
            }
        }
        if (count >= 3) {
            out.collect("[ALERT] Account: " + key + " | Count: " + count + ", Total: " + total);
        }
    }
}
