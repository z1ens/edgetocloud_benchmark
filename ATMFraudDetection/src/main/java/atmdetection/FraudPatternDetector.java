package atmdetection;

import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.streaming.api.windowing.time.Time;


import java.util.List;
import java.util.Map;

public class FraudPatternDetector {

    public static DataStream<String> detectHighRiskSequence(KeyedStream<ATMTransaction, String> keyedStream) {
        // Define CEP pattern: two consecutive high-score transactions within 1 minute
        Pattern<ATMTransaction, ?> pattern = Pattern.<ATMTransaction>begin("first")
                .where(new SimpleCondition<ATMTransaction>() {
                    @Override
                    public boolean filter(ATMTransaction txn) {
                        return txn.getFraudScore() != null && txn.getFraudScore() > 0.9;
                    }
                })
                .next("second")
                .where(new SimpleCondition<ATMTransaction>() {
                    @Override
                    public boolean filter(ATMTransaction txn) {
                        return txn.getFraudScore() != null && txn.getFraudScore() > 0.9;
                    }
                })
                .within(Time.minutes(1));

        PatternStream<ATMTransaction> patternStream = CEP.pattern(keyedStream, pattern);

        return patternStream.select(new PatternSelectFunction<ATMTransaction, String>() {
            @Override
            public String select(Map<String, List<ATMTransaction>> pattern) {
                ATMTransaction first = pattern.get("first").get(0);
                ATMTransaction second = pattern.get("second").get(0);
                return String.format("[CEP ALERT] Account %s had 2 high-score transactions: %.2f, %.2f",
                        first.getAccountId(), first.getFraudScore(), second.getFraudScore());
            }
        });
    }
}
