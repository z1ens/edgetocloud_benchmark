package atmdetection;

import org.apache.flink.api.common.functions.RichMapFunction;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FraudScoreEnrichment extends RichMapFunction<ATMTransaction, ATMTransaction> {
    private final String endpoint;

    public FraudScoreEnrichment(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public ATMTransaction map(ATMTransaction txn) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = "{\"accountId\":\"" + txn.getAccountId() + "\",\"amount\":" + txn.getAmount() + "}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }

        // Simple parse: {"score": 0.92}
        String resp = response.toString();
        double score = Double.parseDouble(resp.replaceAll("[^\\d.]", ""));
        txn.setFraudScore(score);
        return txn;
    }
}
